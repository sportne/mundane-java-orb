# G8-340 Notification Service QoS And Admin Policy Validation

Task ID: G8-340-NOTIFICATION-SERVICE-QOS-ADMIN-POLICY-VALIDATION
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-030, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0019
Specification references: NOT-11
Target module: modules/corba-notification-service
Allowed files: modules/corba-notification-service/src/**, modules/corba-notification-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-340-notification-service-qos-admin-policy-validation.md, docs/roadmap/tasks/g8-350-notification-service-local-delivery.md, README.md
Forbidden files: live peer execution, committed interop reports, peer artifacts, Docker layers, persistent storage, transaction integration, unbounded queues, local delivery routing beyond policy validation tests, IIOP/Naming exposure, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, Java serialization metadata, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add bounded QoS/admin policy models and validators for channel, admin, proxy, queue, and filter limits. Accept only the supported local policy keys and reject unsupported, duplicate, malformed, conflicting, or out-of-range properties with deterministic diagnostics. Do not add durable subscriptions or runtime delivery guarantees beyond validation.
Tests to add/update: Add unit tests for default policies, caller-configured limits, unsupported/duplicate/out-of-range policy rejection, stable diagnostics, and package documentation.
Documentation to update: Notification README, services design, optional services conformance/review, roadmap index, README, and G8-350 status.
Commands to run: ./gradlew :modules:corba-notification-service:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: QoS/admin validation is bounded and deterministic; no durable storage, transaction integration, IIOP/Naming, interop, or live peer claim is added; G8-350 is promoted after completion.
Rollback notes: Revert policy sources, tests, docs, and roadmap status together.
