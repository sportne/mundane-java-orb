# G8-500 Transaction Service Task Group

Task ID: G8-500-TRANSACTION-SERVICE-TASK-GROUP
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-040, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0021
Specification references: TRANS-14
Target module: roadmap, modules/corba-transaction-service, modules/corba-services-core
Allowed files: modules/corba-transaction-service/README.md, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/requirements/service-requirements.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-5*.md, README.md
Forbidden files: modules/**/src/**, runtime implementation, public API implementation, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated artifacts outside explicit later tasks, durable recovery logs before approval, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation planning. Split Transaction Service / OTS into narrow promoted slices before runtime work. Create staged tasks for the coordinator/resource model, timeout policy, local state transitions, propagation metadata, recovery boundary, IIOP request-context boundary, Native Image smoke, structured interop metadata, and conformance closure. Promote only G8-510 for implementation.
Tests to add/update: No product tests in the task-group split. Add task-shape validation by running the design-control pack.
Documentation to update: Services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and module READMEs.
Commands to run: ./gradlew :modules:corba-transaction-service:test :modules:corba-services-core:test :modules:corba-native-image:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Transaction Service implementation remains split into narrow promoted slices; recovery and persistence remain explicit; no peer distributed transaction claim is added early.
Completion evidence: Completed as the G8-510 through G8-590 split. The sequence stages a bounded coordinator/resource model, timeout policy, local state transitions, propagation metadata, recovery boundary, IIOP request-context boundary, Native Image smoke, interop metadata, and conformance closure. G8-510 is promoted as the only ready Transaction Service implementation task; durable recovery logs, XA integration, Security Service integration, distributed peer two-phase commit, and live peer claims remain out of scope.
Rollback notes: Revert Transaction Service implementation slices, tests, docs, and roadmap status changes together.
