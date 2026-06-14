# G8-350 Notification Service Local Delivery

Task ID: G8-350-NOTIFICATION-SERVICE-LOCAL-DELIVERY
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-030, REQ-SVC-020, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0019
Specification references: NOT-11, EVNT-12
Target module: modules/corba-notification-service, modules/corba-event-service
Allowed files: modules/corba-notification-service/src/**, modules/corba-notification-service/README.md, modules/corba-event-service/src/**, modules/corba-event-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-350-notification-service-local-delivery.md, docs/roadmap/tasks/g8-360-notification-service-iiop-naming-exposure.md, README.md
Forbidden files: live peer execution, committed interop reports, peer artifacts, Docker layers, persistent storage, transaction integration, unbounded queues, IIOP/Naming exposure, Native Image smoke, interop metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, Java serialization metadata, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add in-JVM structured push/pull delivery for the supported Notification Service subset. Apply bounded filters and policies from earlier tasks, enforce queue/fan-out limits, remove or diagnose failed/stale local consumers deterministically, and preserve the Event Service compatibility boundary. Do not add network exposure.
Tests to add/update: Add unit tests for local push and pull delivery, filter acceptance/rejection, queue limit diagnostics, failed consumer handling, stale proxy diagnostics, destroyed channel behavior, and package documentation.
Documentation to update: Notification/Event READMEs as needed, services design, optional services conformance/review, roadmap index, README, and G8-360 status.
Commands to run: ./gradlew :modules:corba-notification-service:test :modules:corba-event-service:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Supported structured events route locally through bounded filters and queues with deterministic diagnostics; no persistence, IIOP/Naming, Native Image, interop, or live peer claim is added; G8-360 is promoted after completion.
Completion evidence: Completed with in-JVM structured push delivery, queued pull delivery, pull-supplier delivery, bounded filter routing, queue-limit diagnostics, failed-consumer removal, stale-proxy diagnostics, destroyed-channel diagnostics, and package documentation updates. Unit coverage exercises local push/pull delivery, filter acceptance/rejection, queue overflow, failed consumers, stale proxies, and destroyed-channel behavior. IIOP/Naming, Native Image, interop metadata, persistence, transactions, and live peer claims remain excluded.
Rollback notes: Revert local delivery sources, tests, docs, and roadmap status together.
