# CDR, GIOP, and IIOP Design

## CDR rules

- All reads validate remaining bytes.
- All length fields validate configured maximums before allocation.
- Alignment logic is centralized.
- Endian behavior is explicit.
- Encapsulation handling is separate from top-level message handling.
- Generated codecs call CDR APIs directly.

## CDR Reader/Writer Slices

`modules/corba-cdr` exposes syntax-independent primitive readers and writers.
The first implementation slices cover explicit byte order, primitive alignment,
bounded writer output, deterministic primitive wire encodings, and bounded
length-bearing values.

Primitive coverage includes boolean, octet, char, short, unsigned short, long,
unsigned long, long long, unsigned long long, float, double, and raw 16-octet
long double payloads. Boolean decoding is strict: only octets `0` and `1` are
accepted. Long double numeric interpretation is deferred; this slice preserves
the 16-octet payload.

Length-bearing CDR coverage includes narrow strings, sequence lengths,
fixed-array element-count validation for generated-code loops, raw octet
sequences, length-prefixed encapsulations, and bounded wide strings for the
approved RMI-IIOP value slice. Narrow strings use one-octet Latin-1 mapping in
this CDR slice because code-set negotiation is GIOP-level behavior. Wide
strings are encoded as deterministic UTF-16 code units with a terminating null
code unit and reuse the configured string-octet bound. Encapsulations validate
byte-order markers and create nested readers whose alignment starts at the
encapsulation stream, after the marker has been consumed.

G6-510 adds caller-sized raw octet helpers for protocol layers that already know
the byte count from an enclosing syntax. These helpers do not add length
prefixes and do not introduce GIOP concepts into `corba-cdr`.

G10-040 adds peer-facing helper codecs for wire TypeCodes, Any payloads,
object references, code-set context data, user/system exception reply bodies,
and fragmentable GIOP payloads. ORB invocation behavior remains outside this
wire slice.

The primitive reader/writer package participates in the Native Image validation
lane. Its native smoke executable exercises deterministic CDR primitive,
string, sequence, and encapsulation behavior without reflection, dynamic
proxies, runtime code generation, or native-image metadata.

G7-060 uses these CDR primitives from `corba-rmi-iiop` to prove local
primitive/String value payloads and empty user-exception repository ID payloads.
G7-080 uses the same bounded CDR payloads inside local JVM GIOP/IIOP
request/reply bodies for the approved RMI-IIOP binding slice. It does not claim
external peer interoperability or full CORBA system-exception wire semantics.

## GIOP rules

- Every message kind requires golden-wire tests.
- Fragmentation must be bounded.
- Service contexts must be parsed with configurable limits.
- Unknown or unsupported message behavior must be profile-defined.

## GIOP 1.2 Message Slice

`modules/corba-giop` now provides a bounded in-memory GIOP 1.2 message model and
byte round-trip layer. The reader validates the fixed `GIOP` magic, version,
flags, message type, exact declared message size, complete body consumption for
typed messages, and configured message/body/service-context limits before it
returns a message value.

The supported syntax covers request, reply, cancel request, locate request,
locate reply, close connection, message error, and fragment messages. G10-040
extends request and locate request bodies to support KeyAddr, ProfileAddr, and
ReferenceAddr target addresses. Service contexts remain preserved as opaque
`context_id` plus `context_data` bytes, with an additional code-set context
helper for the approved narrow/wide string code sets. Fragment messages preserve
the GIOP 1.2 request id and raw fragment payload; bounded local assembly is
available for request, reply, and locate-reply payloads.

This slice intentionally has no ORB dispatch, no POA object lookup, no peer
interop execution, and no semantic interpretation of service contexts beyond
the explicit code-set helper. G10-040 adds deterministic user/system exception
reply body codecs, but exception mapping remains owned by later ORB/POA tasks.
G7-080 adds a narrow `corba-rmi-iiop` owner for approved RMI-IIOP reply bodies
without moving that semantic ownership into `corba-giop`. The boundary is:

```text
complete byte array -> GiopMessageReader -> typed GIOP message
typed GIOP message -> GiopMessageWriter -> complete byte array
```

## IIOP rules

- TCP transport must support timeouts and backpressure.
- TLS/mTLS support must be explicit and testable.
- Connection pooling and request correlation must be observable.

## IIOP TCP Slice

`modules/corba-iiop` now provides a local loopback TCP transport for complete
GIOP request/reply messages. The client opens one reusable socket, writes one
bounded GIOP Request at a time, reads a complete GIOP Reply frame, and verifies
the reply request id matches the outstanding request.

The server binds a loopback-capable endpoint, accepts TCP connections until
closed, handles each accepted connection on its own thread, and supports
multiple sequential request/reply cycles on one connection. Frame reads validate
the fixed GIOP header size and configured message/body limits before allocating
the declared body, then delegate message parsing to `GiopMessageReader`.

G6-530 adds endpoint-local TLS and mTLS configuration to the same client/server
entrypoints. `IiopOptions` carries an explicit `SSLContext` for TLS modes plus
optional protocol and cipher-suite filters. The transport creates
`SSLSocket`/`SSLServerSocket` instances from that context only; it does not
read or mutate JVM-global TLS defaults. mTLS is represented by the server
requiring client authentication and the client presenting key material through
its configured context.

This slice intentionally has no connection pooling, generic ORB dispatch,
generic POA lookup, Naming Service behavior, peer interop, CORBA Security
Service enforcement, or hostname/SAN verification policy. G10-040 adds IOR TLS
tagged component payload codecs and bounded fragment assembly in the frame
reader; it does not interpret TLS policy or dispatch requests. G7-080 layers
bounded local JVM RMI-IIOP wire calls above these client/server entrypoints;
peer behavior remains outside the IIOP transport slice. G8-660 is reserved for
the descriptor-backed loopback Security Service / CSIv2 boundary and may add
bounded service-context/tagged-component handling without automatic TLS policy
changes or live peer claims. The current boundary is:

```text
GiopRequest -> IiopClient -> TCP/TLS loopback -> IiopServer -> IiopRequestHandler -> GiopReply
IiopOptions -> SSLContext -> SSLSocket/SSLServerSocket -> GIOP frame exchange
```

## Persistent IOR Direction

ADR-0014 accepts persistent IOR support as a staged follow-on to durable ORB
and POA identity. Persistent IORs must carry bounded durable object keys through
the existing IOR, GIOP target-address, and IIOP dispatch paths. The binary and
stringified forms must preserve the key without relying on process-local
registries.

G12-110 defines the project-owned durable object-key value used by those later
IORs. The key bytes are opaque to the IOR layer but have a bounded `MJOK`
version 1 structure owned by ORB/POA runtime identity code: configured ORB id,
stable POA path components, object id bytes, and one-octet flags.

G12-130 implements the local loopback persistent IOR slice. When a
`LocalObjectReference` carries durable POA key metadata, `IiopObjectReference`
emits the encoded `MJOK` bytes as the IIOP profile object key; transient local
references continue to use the existing ASCII object-id key. The IOR and
stringified IOR layers still treat object keys as opaque octets, so binary and
`IOR:` forms preserve durable keys without depending on ORB-core classes.

The IIOP server dispatch map is keyed by opaque `ObjectKey` values rather than
ASCII strings. KeyAddr, ProfileAddr, and ReferenceAddr all route through the
same byte-preserving lookup path, which allows a stringified persistent IOR to
survive a local ORB/POA/server restart when the caller recreates the same
durable ORB id, POA path, object id, and endpoint. Malformed durable-key
prefixes are rejected before dispatch with deterministic system-exception
replies; stale or unbound durable keys remain `OBJECT_NOT_EXIST`.

G13-010 verifies that restart behavior across separate JVM processes, not only
same-process object recreation. The old `IOR:` string continues to carry the
original opaque object-key octets, and the restarted IIOP server routes them
only when the durable ORB id, POA path, object id, endpoint, and active binding
match the original server process.

G13-090 implements that boundary with a caller-configured durable resolver on
`IiopOrbServerHandler`. The handler first checks exact opaque object-key
bindings, then passes unknown key bytes to the resolver without inspecting
`MJOK`. ORB/POA code owns durable-key decoding, version validation, ORB id
checks, POA path registry lookup, adapter activation, servant-manager policy,
and stale-object diagnostics. Dynamic resolver results use descriptor-level
operation bindings, so the protocol module can dispatch generated operations
through `LocalOrb` without depending on `corba-poa`. The loopback evidence
covers KeyAddr, ProfileAddr, ReferenceAddr, stringified IOR restart,
adapter-activation lookup, servant-manager dispatch, wrong ORB, stale object
ids, malformed keys, and unregistered paths.
