# G8-200 Event Service Task Group

Task ID: G8-200-EVENT-SERVICE-TASK-GROUP
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-020, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0018
Specification references: EVNT-12
Target module: modules/corba-event-service, modules/corba-services-core
Allowed files: modules/corba-event-service/src/**, modules/corba-event-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, modules/corba-native-image/src/**, modules/corba-interop-testkit/src/**, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-200-event-service-task-group.md, README.md
Forbidden files: unrelated service implementation, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated artifacts outside explicit later tasks, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Blocked until maintainers promote an Event Service slice. Implement in staged steps: event-channel model, local push/pull delivery, bounded fan-out/backpressure, optional IIOP/Naming exposure, Native Image smoke, structured interop metadata, and conformance closure.
Tests to add/update: Add channel lifecycle, push/pull, backpressure, disconnect, and failed-consumer tests per promoted slice; add Native Image and interop metadata tests when entrypoints exist.
Documentation to update: Services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and module READMEs.
Commands to run: ./gradlew :modules:corba-event-service:test :modules:corba-services-core:test :modules:corba-native-image:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Event Service implementation remains split into narrow promoted slices; Notification Service dependencies remain explicit; no durable queue or live peer claim is added early.
Rollback notes: Revert Event Service implementation slices, tests, docs, and roadmap status changes together.
