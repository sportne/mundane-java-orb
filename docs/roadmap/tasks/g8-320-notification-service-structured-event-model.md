# G8-320 Notification Service Structured Event Model

Task ID: G8-320-NOTIFICATION-SERVICE-STRUCTURED-EVENT-MODEL
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-030, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0019
Specification references: NOT-11
Target module: modules/corba-notification-service
Allowed files: modules/corba-notification-service/src/**, modules/corba-notification-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-320-notification-service-structured-event-model.md, docs/roadmap/tasks/g8-330-notification-service-bounded-filter-model.md, README.md
Forbidden files: live peer execution, committed interop reports, peer artifacts, Docker layers, Event Service runtime changes except documentation references, local delivery routing, filter evaluation, QoS runtime behavior, persistent storage, generated OMG compatibility APIs, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add an immutable structured-event model for the supported local NOT-11 subset, including fixed event identity fields, primitive named filter properties, bounded variable header/body fields, and deterministic validation for unsupported, malformed, duplicate, or oversized data. Do not add delivery semantics.
Tests to add/update: Add unit tests for valid structured events, primitive property support, duplicate and oversized field rejection, unsupported value diagnostics, immutability, and package documentation.
Documentation to update: Notification README, services design, optional services conformance/review, roadmap index, README, and G8-330 status.
Commands to run: ./gradlew :modules:corba-notification-service:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Structured events are explicit, bounded, immutable, and Native Image friendly; no delivery, filtering, QoS, persistence, IIOP/Naming, interop, or live peer claim is added; G8-330 is promoted after completion.
Rollback notes: Revert structured-event sources, tests, docs, and roadmap status together.
