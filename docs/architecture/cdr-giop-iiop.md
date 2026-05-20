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

Negotiated code sets, TypeCode, object-reference, GIOP, IIOP, and ORB invocation
behavior remain outside the current CDR slice.

The primitive reader/writer package participates in the Native Image validation
lane. Its native smoke executable exercises deterministic CDR primitive,
string, sequence, and encapsulation behavior without reflection, dynamic
proxies, runtime code generation, or native-image metadata.

G7-060 uses these CDR primitives from `corba-rmi-iiop` to prove local
primitive/String value payloads and empty user-exception repository ID payloads.
That RMI slice deliberately stops before ORB dispatch, POA adapters, GIOP reply
body ownership, IIOP sockets, or peer interoperability claims.

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
locate reply, close connection, message error, and fragment messages. Request
and locate request bodies support only KeyAddr target addresses in this slice;
ProfileAddr and ReferenceAddr are rejected until object-reference integration.
Service contexts are preserved as opaque `context_id` plus `context_data` bytes.
Fragment messages preserve the GIOP 1.2 request id and raw fragment payload, and
the header more-fragments flag remains visible on the message header.

This slice intentionally has no TCP listener, no client connection management,
no ORB dispatch, no POA object lookup, no peer interop, no GIOP reply exception
marshaling, and no semantic interpretation of service contexts. The boundary is:

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

This slice intentionally has no connection pooling, ORB dispatch, POA lookup,
generated stubs or skeletons, Naming Service behavior, peer interop, CORBA
Security Service, TLS tagged components in IORs, hostname/SAN verification
policy, or GIOP exception-body marshaling. The boundary is:

```text
GiopRequest -> IiopClient -> TCP/TLS loopback -> IiopServer -> IiopRequestHandler -> GiopReply
IiopOptions -> SSLContext -> SSLSocket/SSLServerSocket -> GIOP frame exchange
```
