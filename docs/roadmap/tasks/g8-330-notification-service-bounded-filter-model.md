# G8-330 Notification Service Bounded Filter Model

Task ID: G8-330-NOTIFICATION-SERVICE-BOUNDED-FILTER-MODEL
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-030, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0019
Specification references: NOT-11
Target module: modules/corba-notification-service
Allowed files: modules/corba-notification-service/src/**, modules/corba-notification-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-330-notification-service-bounded-filter-model.md, docs/roadmap/tasks/g8-340-notification-service-qos-admin-policy-validation.md, README.md
Forbidden files: live peer execution, committed interop reports, peer artifacts, Docker layers, general scripting engines, reflection metadata, dynamic proxies, runtime bytecode generation, persistent storage, local delivery routing beyond isolated evaluator tests, IIOP/Naming exposure, Java serialization metadata, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add an explicit bounded filter parser/evaluator for the supported structured-event model. Support deterministic boolean constants, equality/inequality over event identity and primitive named filter properties, bounded boolean composition, expression length/depth/term limits, and stable rejection diagnostics. Do not use scripting, reflection dispatch, or runtime code generation.
Tests to add/update: Add parser/evaluator tests for accepted expressions, unsupported operators, malformed syntax, depth/term/length limits, unknown properties, type mismatch diagnostics, deterministic evaluation, and package documentation.
Documentation to update: Notification README, services design, optional services conformance/review, roadmap index, README, and G8-340 status.
Commands to run: ./gradlew :modules:corba-notification-service:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Filter parsing and evaluation are bounded, deterministic, closed-world friendly, and isolated from delivery; no QoS, persistence, IIOP/Naming, interop, or live peer claim is added; G8-340 is promoted after completion.
Rollback notes: Revert filter sources, tests, docs, and roadmap status together.
