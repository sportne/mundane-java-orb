# G8-300 Notification Service Task Group

Task ID: G8-300-NOTIFICATION-SERVICE-TASK-GROUP
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-030, REQ-SVC-020, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0019
Specification references: NOT-11, EVNT-12
Target module: roadmap, modules/corba-notification-service
Allowed files: modules/corba-notification-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-3*.md, README.md
Forbidden files: modules/**/src/**, runtime implementation, public API implementation, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated artifacts outside explicit later tasks, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation planning. Promoted by maintainer directive for the task-splitting slice. Split Notification Service into staged implementation tasks covering Event Service compatibility boundary, structured events, bounded filters, QoS/admin policy, local delivery, optional IIOP/Naming exposure, Native Image smoke, structured interop metadata, and conformance closure. Promote only G8-310 for implementation.
Tests to add/update: No product tests in the task-group split. Add task-shape validation by running the design-control pack.
Documentation to update: Services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, module README, and generated Notification Service task files.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Notification Service implementation remains split into narrow promoted slices; filter evaluation is bounded; no persistent event storage or live peer claim is added early.
Completion evidence: Completed as the G8-310 through G8-390 split. The sequence stages Event Service compatibility boundary, structured event model, bounded filter model, QoS/admin policy validation, local delivery, loopback IIOP/Naming exposure, Native Image smoke, interop metadata, and conformance closure. G8-310 is promoted as the only ready Notification Service implementation task; live peer execution remains out of scope and human-gated.
Rollback notes: Revert Notification Service implementation slices, tests, docs, and roadmap status changes together.
