# Services Design

## Core service

CosNaming is treated as a core service because it is central to practical ORB
object discovery.

The core service boundary is deliberately narrow. Shared service support may
hold common diagnostics, limit configuration, service descriptor conventions,
and test-fixture utilities after a service gate approves those surfaces. It must
not duplicate ORB, POA, IIOP, CDR, interceptor, or generated-code behavior that
belongs to the existing runtime and compiler modules.

ADR-0014 accepts Naming persistence only after durable IORs are implemented.
The persistent Naming store must hold bounded, versioned naming context and
durable IOR data, not servant instances, process-local object references, Java
serialized objects, or peer-derived artifacts. Store locations must be
caller-configured and validated; the runtime must not silently create a global
database.

G12-140 implements that persistence as an explicit `NetworkNamingService`
startup option. Callers provide a durable `OrbIdentity`, a single store file
path, and bounded store limits through `NamingPersistenceOptions`; existing
in-memory startup remains unchanged. Persistent Naming contexts use durable
`MJOK` object keys and the store file uses `MJNS` version 1 records for context
IORs, name entries, target kinds, and stringified durable target IORs. The
server rewrites the whole store atomically through a sibling temporary file
after successful bind, rebind, unbind, bind-new-context, and destroy
operations. It rejects corrupted or oversized stores, traversal-containing store
paths, transient IORs, malformed durable object keys, and wrong-ORB durable
targets before committing state.

G13-010 adds process-level evidence for that caller-managed model. A forked
Naming server process creates a durable binding and persistent child context,
exits, and a second server process with the same durable ORB id, store, and
endpoint resolves the original `corbaname` value through the old location. A
restart with a different configured ORB id rejects the existing store during
startup rather than silently adopting another identity.

G13-020 hardens the store write path without changing `MJNS` version 1. Existing
directory store paths are rejected deterministically, temp-file contents are
forced before replacement when the JDK and filesystem support it, failed writes
attempt best-effort sibling temp cleanup, and filesystems without atomic moves
fall back to bounded replacement. Store backups, retention, file permissions,
and external replication remain caller/operator responsibilities unless a later
roadmap task adds an explicit policy.

G13-030 records the `MJNS` version 1 compatibility policy. The store is a
single big-endian binary file with magic `MJNS`, one-octet version `1`, a
length-prefixed UTF-8 durable ORB id, signed 32-bit next-context id, unsigned
16-bit context count, and one record per context. Each context record contains a
length-prefixed stringified durable NamingContext IOR, a one-octet destroyed
flag, an unsigned 16-bit binding count, and one record per binding. Each
binding record contains length-prefixed UTF-8 name id and kind strings, a
one-octet target kind (`0` object, `1` context), and a length-prefixed
stringified durable target IOR. All string, store, context, and binding counts
remain bounded by `NamingPersistenceOptions`.

No `MJNS` migrations are implemented yet. Unsupported future versions, trailing
octets, malformed UTF-8, oversized records, wrong ORB ids, wrong Naming context
repository ids, wrong context key namespaces, malformed durable keys, transient
target IORs, and wrong-ORB target IORs fail deterministically during startup or
before a mutating bind is committed.

G13-050 defines future persistent Naming peer scenarios without approving live
execution. The proposed peer claim is that an approved black-box peer client can
use an old persistent Naming IOR or `corbaname` value after our Naming server
process restarts with the same durable ORB id, endpoint, and `MJNS` store. The
peer is not expected to understand `MJNS` or `MJOK`; it only preserves ordinary
IOR, object-key, and Naming URL data on the wire. Raw Naming stores, reports,
logs, IORs, peer artifacts, Docker layers, and Native Image binaries remain
ignored local outputs unless a later human-gated task approves a clean-room
summary.

## Optional services

| Service | Requirement | Spec key | Module boundary | Current posture |
|---|---|---|---|---|
| Time Service | REQ-SVC-060 | TIME-11 | `modules/corba-time-service` owns time and interval value models, caller-configured clock policy, local query behavior, and future Naming/IIOP exposure. | Design accepted by ADR-0017; implementation blocked behind `G8-100-TIME-SERVICE-TASK-GROUP`. |
| Event Service | REQ-SVC-020 | EVNT-12 | `modules/corba-event-service` owns event channels, push/pull supplier and consumer surfaces, channel lifecycle, and bounded fan-out/backpressure. | Design accepted by ADR-0018; implementation blocked behind `G8-200-EVENT-SERVICE-TASK-GROUP`. |
| Notification Service | REQ-SVC-030 | NOT-11 | `modules/corba-notification-service` owns notification channels, structured events, bounded filtering, QoS/admin models, and Event Service compatibility boundaries. | Design accepted by ADR-0019; implementation blocked behind `G8-300-NOTIFICATION-SERVICE-TASK-GROUP`. |
| Trading Service | REQ-SVC-010 | TRADE-10 | `modules/corba-trading-service` owns service type repositories, offer repositories, constraint parsing/evaluation, local query, and future import/export behavior. | Design accepted by ADR-0020; implementation blocked behind `G8-400-TRADING-SERVICE-TASK-GROUP`. |
| Transaction Service / OTS | REQ-SVC-040 | TRANS-14 | `modules/corba-transaction-service` owns transaction coordinator, resource, propagation, timeout, and recovery design decisions. | Design accepted by ADR-0021; implementation blocked behind `G8-500-TRANSACTION-SERVICE-TASK-GROUP`. |
| Security Service / CSIv2 | REQ-SVC-050 | SEC-18, CORBA-IOP-SECURITY | `modules/corba-security-service` owns credentials, trust, policy, CSIv2 metadata, local policy evaluation, and audit/failure disclosure. | Design accepted by ADR-0022; implementation blocked behind `G8-600-SECURITY-SERVICE-TASK-GROUP`. |

Each optional service now has an accepted ADR and blocked task group. Runtime
implementation still requires a task group or slice to be promoted to
`ready-for-implementation`.

## Native Image Policy

Optional services inherit the project-wide Native Image posture. Approved
service designs must avoid classpath scanning, runtime bytecode generation,
dynamic proxies, serialization metadata, reflection metadata, process
execution, internal JDK APIs, and `Unsafe` unless a later human-gated exception
documents the exact metadata, risk, and test evidence.

## Interop Posture

No optional service currently claims live peer interoperability. A service gate
must name the peer scenarios, object keys, IDL fixtures, report schema, and
missing-prerequisite behavior before any live peer execution is required.
Default evidence may remain structured-report-only until approved caches,
digest-pinned base images, and container runtime inputs are available.

## Security Review Expectations

Every optional service gate must record hostile-input boundaries, resource
limits, authentication or authorization assumptions where relevant, persistence
and recovery assumptions where relevant, and failure diagnostics. Security
Service work also requires a dedicated credential, policy, trust, and CSIv2
review before implementation.
