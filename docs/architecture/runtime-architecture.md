# Runtime Architecture

## ORB core responsibilities

- lifecycle;
- configuration;
- initial references;
- object reference abstraction;
- invocation pipeline;
- interceptor pipeline;
- timeout policy;
- exception mapping;
- shutdown coordination.

## Server path

```text
IIOP listener
  -> GIOP frame decoder
  -> object key lookup
  -> POA / servant resolution
  -> generated skeleton dispatcher
  -> CDR reply encoder
  -> GIOP reply
```

## Client path

```text
generated stub
  -> operation descriptor
  -> CDR request body
  -> GIOP request
  -> IIOP connection
  -> reply correlation
  -> CDR reply body
  -> typed value or exception
```

## Local invocation path

The first G6 local invocation slice is intentionally in-process only:

```text
generated client
  -> LocalOrb.invoke
  -> LocalInvocationDispatcher
  -> generated-style servant
```

Exceptions follow the same in-process path and are normalized before they leave
ORB core:

```text
generated client
  -> LocalOrb.invoke
  -> LocalInvocationDispatcher
  -> generated-style servant
  -> local exception mapper
  -> typed user wrapper or CORBA system exception
```

`corba-modern-api` owns the generated-code-facing request and dispatcher
contracts. `corba-orb-core` owns local object references, local object identity,
dispatcher registration, typed local initial references, lifecycle checks,
exception mapping, and shutdown coordination. `corba-omg-api` owns the minimal
`org.omg.CORBA` exception compatibility surface used by this local slice.

G12-110 adds explicit ORB identity values to this local runtime. The default
`LocalOrb.create()` remains transient and process-local. Callers that need
restart-safe references must choose `LocalOrb.create(OrbIdentity.durable(...))`
with a stable configured `orbId`; durable IDs are never generated implicitly.

## Local Naming Service Path

G6-810 adds a local Naming Service path over in-process initial references:

```text
LocalNamingService.install
  -> LocalOrb.registerInitialReference("NameService")
  -> NamingContext bind / resolve / list / destroy
  -> CorbanameResolver for corbaname:rir:#name
```

This path is an in-memory local JVM slice. It does not contact remote IIOP
addresses, discover services dynamically, or expose legacy CosNaming
compatibility APIs. G10-060 adds the bounded loopback IIOP Naming endpoint and
remote `corbaloc`/`corbaname` resolution. G12-140 adds caller-configured network
Naming persistence for durable IORs and Naming contexts.

This path does not open sockets, construct GIOP messages, use IIOP transport,
invoke POA policy behavior, create dynamic proxies, generate runtime bytecode,
use reflection for dispatch, or marshal exceptions as CDR reply bodies.
Generated-style dispatchers call servants explicitly using static operation
descriptors.

## Local RMI-IIOP Adapter Path

G7-070 adds generated RMI-IIOP adapters over the same in-process ORB and POA
contracts:

```text
generated RMI stub
  -> LocalOrb.invoke
  -> Poa dispatch
  -> generated RMI tie or skeleton
  -> servant
```

This path exposes generated static binding descriptors, operation descriptors,
local stubs, ties, and skeleton activation helpers for the approved RMI-IIOP
slice. It does not discover classes dynamically, create proxies, invoke through
reflection, or marshal values through Java serialization.

## Local JVM RMI-IIOP Wire Path

G7-080 adds a bounded local JVM wire path for the approved RMI-IIOP binding
slice:

```text
generated RMI wire stub
  -> RmiIiopWireClient
  -> IiopClient
  -> GIOP request with KeyAddr object key
  -> IiopServer
  -> RmiIiopWireServerHandler
  -> LocalOrb.invoke
  -> Poa dispatch
  -> generated RMI tie or skeleton
  -> GIOP reply
  -> generated RMI wire stub
```

The wire path supports approved primitive/String/void operation payloads and
empty declared user exceptions by repository ID. It uses existing GIOP/IIOP
transport entrypoints and does not open sockets directly from `corba-rmi-iiop`,
run external peers, claim peer interoperability, scan classpaths, create dynamic
proxies, generate runtime bytecode, or use Java serialization marshaling.

## Network ORB/POA IIOP Dispatch Path

G10-050 adds the first non-RMI network dispatch bridge between bounded IIOP
transport and local ORB/POA activation:

```text
generated or fixture client
  -> IiopOrbClient
  -> GIOP request with KeyAddr/ProfileAddr/ReferenceAddr target
  -> IiopServer
  -> IiopOrbServerHandler
  -> LocalOrb.invoke
  -> POA active servant dispatcher
  -> GIOP normal, user-exception, or system-exception reply
```

The bridge is deliberately syntax/runtime-local. `corba-iiop` owns the network
object-reference wrapper, operation codec contract, client helper, and server
handler. `corba-orb-core` and `corba-poa` continue to own object identity,
lifecycle checks, servant activation, and generated-style dispatch. The bridge
supports deterministic object-key lookup, GIOP 1.2 target-address variants,
request-id correlation through existing `IiopClient`/`IiopServer`, declared user
exception replies by repository ID, and system exception reply bodies.

G12-130 extends the bridge for persistent local references. If a local reference
carries `DurableObjectKey` metadata, `IiopObjectReference` emits those encoded
key octets into the IIOP profile and stringified IOR forms.
`IiopOrbServerHandler` routes by opaque `ObjectKey`, so binary durable keys work
through KeyAddr, ProfileAddr, and ReferenceAddr. Restart-safe dispatch is still
caller-configured: the process must recreate the same durable ORB id, POA path,
object id, servant binding, and endpoint before an old stringified IOR can route
locally.

G13-010 strengthens that claim with forked-JVM evidence. The durable IOR restart
test starts one server process, records its stringified IOR, exits that process,
starts a second process with the same durable ORB id, endpoint, POA path, object
id, and servant binding, and dispatches through the old IOR. Wrong-ORB and
missing-binding restarts remain deterministic unknown-object failures.

This path does not add CORBA Security Service policy, RMI-IIOP value semantics,
live peer harness execution, reflection dispatch, dynamic proxies, runtime
bytecode generation, Java serialization marshaling, or generated production
artifacts.

## Portable Interceptor request flow

G10-080 adds a bounded Portable Interceptor request-flow pipeline around the
implemented ORB/IIOP bridge. `corba-interceptors` owns the interceptor
registration model, request contexts, deterministic callback ordering, service
context mutation rules, and stable diagnostics. `corba-iiop` calls the registry
at the existing client and server request/reply boundaries.

Client `sendRequest` callbacks run in registration order before the GIOP request
is encoded. Client `receiveReply` and `receiveException` callbacks run in
reverse registration order after the reply is decoded or a deterministic local
failure is observed. Server `receiveRequestServiceContexts` and
`receiveRequest` callbacks run in registration order before local ORB dispatch.
Server `sendReply` and `sendException` callbacks run in reverse registration
order before the GIOP reply is emitted.

The pipeline is intentionally explicit and closed-world friendly. Interceptors
are caller-supplied objects, not discovered by classpath scanning or service
loading. They can add or replace GIOP service contexts through bounded value
objects, but they cannot install Security Service policy, change object adapter
lookup semantics, invoke reflection dispatch, generate bytecode, or require
Java serialization metadata.

## Durable Identity Model

ADR-0014 accepts durable ORB and POA identity as the restart-safe object
reference direction for post-G12 runtime work. A durable ORB has an explicit
configured `orbId`; persistent POAs have stable hierarchical paths under that
ORB; and persistent object keys use a bounded, versioned project-owned encoding
that carries `orbId`, POA path, object id bytes, and key flags.

Transient and persistent references remain separate runtime concepts. The ORB
must not silently promote process-local object ids into durable keys, and it
must not create an implicit global persistence location. Follow-on tasks add the
value codecs, persistent POA activation behavior, persistent IOR round-trips,
and Naming persistence in that order.

G12-110 implements the ORB identity value and the durable object-key codec.
The codec uses a bounded `MJOK` version 1 binary envelope and rejects malformed
versions, oversized encoded values, invalid ASCII identifiers, empty paths, and
empty object ids before dispatch.

G12-120 implements persistent POA object keys for retained local activations.
`PERSISTENT` POAs require `LocalOrb.create(OrbIdentity.durable(...))`; retained
`USER_ID` and deterministic per-POA `SYSTEM_ID` activations attach
`DurableObjectKey` metadata to `LocalObjectReference`. Persistent key lookup is
local and diagnostic-only for now: it validates the configured ORB id, POA path,
ASCII object id, and active object map entry before dispatch.

G12-130 preserves durable keys through local IOR creation, binary and
stringified IOR parsing, GIOP target-address extraction, and loopback IIOP
server dispatch. Malformed durable-key prefixes fail before invocation; stale
durable keys fail as unknown object references.

G12-140 completes the staged durable identity implementation by adding explicit
Naming persistence. `NetworkNamingService.bind(..., NamingPersistenceOptions)`
uses a caller-provided durable ORB identity and store path, emits durable
Naming-context object keys, persists only stringified durable IOR/context data
in a bounded `MJNS` file, and rewrites that file atomically after successful
mutating Naming operations. The runtime still does not persist servants,
process-local object references, Java-serialized objects, or peer artifacts.

ADR-0015 approves POA-managed durable rehydration as the next local runtime
direction. The current caller-managed model remains valid: applications may
still recreate POAs and activate servants explicitly before accepting requests.
G13-060 adds the explicit durable POA path registry. Durable ORBs own the
registry, persistent POAs expose narrow wrappers for registering their own
paths, and shutdown clears the approved path set. The remaining follow-on model
adds adapter activation lookup so a valid durable object key can locate or
recreate an approved persistent POA path before normal POA request-processing
policy is applied.

Rehydration remains a dispatch and activation contract, not persistence for
servants or application state. Callers must configure the durable ORB identity,
endpoint policy, Naming store, approved POA paths, adapter activation factories,
servant managers, and all backing state explicitly. The ORB/POA runtime owns
bounded `MJOK` decoding, ORB id and path validation, POA registry lookup,
servant-manager invocation according to POA policy, and deterministic
system-exception mapping for malformed, wrong-ORB, unregistered-path,
inactive-adapter, and stale-object cases.

The IIOP layer continues to treat durable keys as opaque octets. It preserves
KeyAddr, ProfileAddr, ReferenceAddr, binary IOR, and stringified IOR forms, then
routes persistent keys to the ORB/POA durable-key lookup path instead of parsing
`MJOK` itself. All rehydration implementation must remain closed-world friendly:
no reflection metadata, dynamic proxies, classpath scanning, Java serialization,
runtime bytecode generation, `Unsafe`, `sun.*`, or `jdk.internal.*`.

G13-050 defines future live peer durable persistence scenarios as design-only
interop work. The proposed peer-facing claim is that approved black-box peers
preserve opaque object-key octets when acting as clients for our restarted
durable JVM or Native Image servers. Peers are not expected to decode `MJOK`,
inspect `MJNS`, or provide durable server keys for our clients. Actual peer
execution, manifest metadata, raw report capture, and compatibility claims stay
deferred behind a later human gate.
