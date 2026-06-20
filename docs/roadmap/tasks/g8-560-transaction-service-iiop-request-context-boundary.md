# G8-560 Transaction Service IIOP Request Context Boundary

Task ID: G8-560-TRANSACTION-SERVICE-IIOP-REQUEST-CONTEXT-BOUNDARY
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-040, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0021
Specification references: TRANS-14
Target module: modules/corba-transaction-service, modules/corba-services-core, modules/corba-iiop
Allowed files: modules/corba-transaction-service/src/**, modules/corba-transaction-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, modules/corba-iiop/src/**, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-560-transaction-service-iiop-request-context-boundary.md, docs/roadmap/tasks/g8-570-transaction-service-native-smoke.md, README.md
Forbidden files: generated OMG APIs, broad ORB refactors, durable recovery logs, Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, XA integration, Security Service integration, peer distributed two-phase commit claims, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add descriptor-backed loopback IIOP request-context boundary for the supported local propagation subset, including deterministic propagation context encoding/decoding, malformed request-context diagnostics, stale and expired context rejection, and clean shutdown. Do not add distributed peer two-phase commit, broad ORB refactors, generated OMG APIs, recovery logs, Native Image smoke, interop metadata, XA, Security Service, or live peer claims.
Tests to add/update: Add unit tests for request-context encode/decode, loopback propagation metadata handling, malformed context rejection, stale/unknown transaction diagnostics, expired context diagnostics, unknown operation/object diagnostics if a loopback facade is exposed, and clean shutdown.
Documentation to update: Transaction Service README, services design, optional services conformance/review, roadmap index, README, and G8-570 status.
Commands to run: ./gradlew :modules:corba-transaction-service:test :modules:corba-services-core:test :modules:corba-iiop:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Transaction Service exposes a bounded loopback IIOP request-context boundary for local propagation metadata without distributed peer two-phase commit, broad ORB refactors, generated OMG APIs, recovery logs, Native Image, interop, or live peer claims; G8-570 is promoted after completion.
Rollback notes: Revert Transaction Service request-context boundary sources, tests, docs, and roadmap status together.
