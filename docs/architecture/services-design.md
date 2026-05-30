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

## Optional services

| Service | Requirement | Spec key | Module boundary | Current posture |
|---|---|---|---|---|
| Trading Service | REQ-SVC-010 | TRADE-10 | `modules/corba-trading-service` owns trader-specific APIs, matching policy, offer repositories, and future generated bindings. | Deferred behind `G8-D10-TRADING-SERVICE-DESIGN-GATE`. |
| Event Service | REQ-SVC-020 | EVNT-12 | `modules/corba-event-service` owns event channels, push/pull supplier and consumer surfaces, and channel lifecycle behavior. | Deferred behind `G8-D20-EVENT-SERVICE-DESIGN-GATE`. |
| Notification Service | REQ-SVC-030 | NOT-11 | `modules/corba-notification-service` owns notification channels, filtering, QoS/admin models, and any relationship to Event Service compatibility. | Deferred behind `G8-D30-NOTIFICATION-SERVICE-DESIGN-GATE`. |
| Transaction Service / OTS | REQ-SVC-040 | TRANS-14 | `modules/corba-transaction-service` owns transaction coordinator, resource, propagation, and recovery design decisions. | Deferred behind `G8-D40-TRANSACTION-SERVICE-DESIGN-GATE`. |
| Security Service / CSIv2 | REQ-SVC-050 | SEC-18, CORBA-IOP-SECURITY | `modules/corba-security-service` owns security-service APIs, credential and policy boundaries, and CSIv2-specific design after approval. | Deferred behind `G8-D50-SECURITY-SERVICE-DESIGN-GATE`. |
| Time Service | REQ-SVC-060 | TIME-11 | `modules/corba-time-service` owns time and interval services, clock source policy, and tolerance handling. | Deferred behind `G8-D60-TIME-SERVICE-DESIGN-GATE`. |

Each optional service must have a separate design document, requirement set,
interop plan, and security review before implementation.

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
