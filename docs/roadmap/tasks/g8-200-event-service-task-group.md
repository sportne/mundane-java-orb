# G8-200 Event Service Task Group

Task ID: G8-200-EVENT-SERVICE-TASK-GROUP
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-020, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0018
Specification references: EVNT-12
Target module: roadmap, modules/corba-event-service, modules/corba-services-core
Allowed files: docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-2*.md, README.md
Forbidden files: modules/**/src/**, runtime implementation, public API implementation, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated artifacts outside explicit later tasks, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation planning. Split the Event Service task group into narrow implementation slices before any runtime behavior is implemented. The split must preserve the staged order: event-channel model, local push/pull delivery, bounded fan-out/backpressure, optional IIOP/Naming exposure, Native Image smoke, structured interop metadata, and conformance closure.
Tests to add/update: No product tests in the task-group split. Add task-shape validation by running the design-control pack.
Documentation to update: Roadmap index, README, and generated Event Service task files only.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Event Service implementation is split into narrow follow-on tasks; at most the first non-runtime or local-foundation slice is promoted to ready-for-implementation; Notification Service dependencies remain explicit; no durable queue or live peer claim is added early.
Completion evidence: Completed as the G8-210 through G8-270 split. The sequence stages local channel model, local push/pull delivery, bounded fan-out/backpressure, loopback IIOP/Naming exposure, Native Image smoke, structured interop metadata, and local conformance closure. G8-210 is promoted as the only ready Event Service implementation task; live peer execution remains out of scope and human-gated.
Rollback notes: Revert Event Service implementation slices, tests, docs, and roadmap status changes together.
