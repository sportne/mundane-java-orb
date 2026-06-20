# G8-530 Transaction Service Local State Transitions

Task ID: G8-530-TRANSACTION-SERVICE-LOCAL-STATE-TRANSITIONS
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-040, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0021
Specification references: TRANS-14
Target module: modules/corba-transaction-service, modules/corba-services-core
Allowed files: modules/corba-transaction-service/src/**, modules/corba-transaction-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-530-transaction-service-local-state-transitions.md, docs/roadmap/tasks/g8-540-transaction-service-propagation-metadata.md, README.md
Forbidden files: propagation metadata, recovery boundary, IIOP request-context integration, Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, durable recovery logs, XA integration, Security Service integration, peer distributed two-phase commit claims, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add local transaction state transitions for active, rollback-only, committed, rolled-back, timeout-rolled-back, and bounded heuristic states. Add local prepare, commit, and rollback callback handling for enlisted resources, deterministic rollback, heuristic, timeout, stale-resource, and resource-failure diagnostics, and cleanup behavior. Do not add propagation, IIOP, recovery logs, peer distributed two-phase commit, Native Image, interop, XA, Security Service, or live peer behavior.
Tests to add/update: Add unit tests for successful local commit and rollback, rollback-only behavior, timeout rollback, prepare failure rollback, commit/rollback resource failure diagnostics, heuristic diagnostics, stale resource handling, cleanup after terminal states, illegal transition rejection, and deterministic callback ordering.
Documentation to update: Transaction Service README, services design, optional services conformance/review, roadmap index, README, and G8-540 status.
Commands to run: ./gradlew :modules:corba-transaction-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Transaction Service exposes deterministic local coordinator/resource state transitions and diagnostics without propagation, IIOP, recovery logs, Native Image, interop, distributed peer two-phase commit, or live peer claims; G8-540 is promoted after completion.
Rollback notes: Revert Transaction Service local state transition sources, tests, docs, and roadmap status together.
