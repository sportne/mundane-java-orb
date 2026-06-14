# G8-310 Notification Service Event Compatibility Boundary

Task ID: G8-310-NOTIFICATION-SERVICE-EVENT-COMPATIBILITY-BOUNDARY
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-030, REQ-SVC-020, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0019
Specification references: NOT-11, EVNT-12
Target module: modules/corba-notification-service, modules/corba-event-service, modules/corba-services-core
Allowed files: modules/corba-notification-service/src/**, modules/corba-notification-service/README.md, modules/corba-event-service/src/**, modules/corba-event-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-310-notification-service-event-compatibility-boundary.md, docs/roadmap/tasks/g8-320-notification-service-structured-event-model.md, README.md
Forbidden files: live peer execution, committed interop reports, peer artifacts, Docker layers, generated OMG compatibility APIs, broad structured-event delivery, filter parsing/evaluation, QoS runtime behavior, persistent storage, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add the local Notification Service channel lifecycle and Event Service compatibility boundary. Provide bounded options, deterministic diagnostics, supplier/consumer admin handles, proxy handles, channel destruction behavior, and explicit adapters or model hooks needed for later structured-event work without adding filtering, QoS behavior, broad local delivery, IIOP/Naming exposure, Native Image smoke, or interop metadata.
Tests to add/update: Add unit tests for channel creation/destruction, admin/proxy ownership, configured limit rejection, destroyed-state diagnostics, Event Service compatibility boundary identity, and package documentation.
Documentation to update: Notification/Event/Services Core READMEs as needed, services design, optional services conformance/review, roadmap index, README, and G8-320 status.
Commands to run: ./gradlew :modules:corba-notification-service:test :modules:corba-event-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Notification Service exposes a bounded local channel/admin/proxy lifecycle aligned with Event Service compatibility expectations; no structured delivery, filtering, QoS, persistence, IIOP/Naming, Native Image, or live peer claim is added; G8-320 is promoted after completion.
Rollback notes: Revert Notification Service compatibility-boundary sources, tests, docs, and roadmap status together.
