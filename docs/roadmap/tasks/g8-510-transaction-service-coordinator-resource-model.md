# G8-510 Transaction Service Coordinator Resource Model

Task ID: G8-510-TRANSACTION-SERVICE-COORDINATOR-RESOURCE-MODEL
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-040, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0021
Specification references: TRANS-14
Target module: modules/corba-transaction-service, modules/corba-services-core
Allowed files: modules/corba-transaction-service/build.gradle, modules/corba-transaction-service/src/**, modules/corba-transaction-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/requirements/service-requirements.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-510-transaction-service-coordinator-resource-model.md, docs/roadmap/tasks/g8-520-transaction-service-timeout-policy.md, README.md
Forbidden files: timeout policy, completion/state-transition behavior, propagation metadata, recovery boundary, IIOP request-context integration, Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, durable recovery logs, XA integration, Security Service integration, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add bounded local coordinator and resource models with transaction IDs, resource IDs, coordinator and resource handles, immutable snapshots, enlist and delist behavior, duplicate, missing, stale-resource, malformed identifier, and configured resource/transaction limit diagnostics. Do not add timeout, completion, propagation, recovery, IIOP, Native Image, interop, XA, durable log, Security Service, distributed two-phase commit, or live peer behavior.
Tests to add/update: Add unit tests for transaction creation, lookup, snapshot immutability, resource enlistment, delistment, duplicate resource rejection, missing/stale resource diagnostics, malformed identifiers, transaction/resource limit enforcement, deterministic ordering, and package documentation.
Documentation to update: Transaction Service and Services Core READMEs as needed, services design, optional services conformance/review, service requirements, roadmap index, README, and G8-520 status.
Commands to run: ./gradlew :modules:corba-transaction-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Transaction Service exposes a bounded local coordinator/resource model with deterministic diagnostics and no timeout, completion, propagation, recovery, IIOP, Native Image, interop, durable log, XA, Security Service, distributed two-phase commit, or live peer claim; G8-520 is promoted after completion.
Rollback notes: Revert Transaction Service coordinator/resource model sources, tests, docs, and roadmap status together.
