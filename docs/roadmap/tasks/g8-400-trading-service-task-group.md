# G8-400 Trading Service Task Group

Task ID: G8-400-TRADING-SERVICE-TASK-GROUP
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-010, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0020
Specification references: TRADE-10
Target module: modules/corba-trading-service, modules/corba-services-core
Allowed files: modules/corba-trading-service/src/**, modules/corba-trading-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, modules/corba-native-image/src/**, modules/corba-interop-testkit/src/**, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-400-trading-service-task-group.md, README.md
Forbidden files: unrelated service implementation, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated artifacts outside explicit later tasks, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Split Trading Service into narrow promoted slices before runtime work. Implement in staged steps: service type repository, offer repository, bounded constraint parser/evaluator, local query, import/export design, optional IIOP/Naming exposure, Native Image smoke, structured interop metadata, and conformance closure.
Tests to add/update: Add service type, offer repository, constraint parser hostile-input, local query, Native Image, and interop metadata tests per promoted slice.
Documentation to update: Services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and module READMEs.
Commands to run: ./gradlew :modules:corba-trading-service:test :modules:corba-services-core:test :modules:corba-native-image:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Trading Service implementation remains split into narrow promoted slices; constraint evaluation is bounded; no durable offer persistence or live peer claim is added early.
Rollback notes: Revert Trading Service implementation slices, tests, docs, and roadmap status changes together.
