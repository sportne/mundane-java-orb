# G8-500 Transaction Service Task Group

Task ID: G8-500-TRANSACTION-SERVICE-TASK-GROUP
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-040, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0021
Specification references: TRANS-14
Target module: modules/corba-transaction-service, modules/corba-services-core
Allowed files: modules/corba-transaction-service/src/**, modules/corba-transaction-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, modules/corba-native-image/src/**, modules/corba-interop-testkit/src/**, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-500-transaction-service-task-group.md, README.md
Forbidden files: unrelated service implementation, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated artifacts outside explicit later tasks, durable recovery logs before approval, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Blocked until maintainers promote a Transaction Service slice. Implement in staged steps: coordinator/resource model, timeout policy, local state transitions, propagation metadata, recovery design, Native Image smoke, structured interop metadata, and conformance closure.
Tests to add/update: Add coordinator/resource, timeout, rollback/heuristic diagnostics, propagation metadata, Native Image, and interop metadata tests per promoted slice.
Documentation to update: Services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and module READMEs.
Commands to run: ./gradlew :modules:corba-transaction-service:test :modules:corba-services-core:test :modules:corba-native-image:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Transaction Service implementation remains split into narrow promoted slices; recovery and persistence remain explicit; no peer distributed transaction claim is added early.
Rollback notes: Revert Transaction Service implementation slices, tests, docs, and roadmap status changes together.
