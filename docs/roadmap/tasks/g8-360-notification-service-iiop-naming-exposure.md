# G8-360 Notification Service IIOP And Naming Exposure

Task ID: G8-360-NOTIFICATION-SERVICE-IIOP-NAMING-EXPOSURE
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-030, REQ-SVC-020, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0019
Specification references: NOT-11, EVNT-12
Target module: modules/corba-notification-service, modules/corba-event-service
Allowed files: modules/corba-notification-service/src/**, modules/corba-notification-service/README.md, modules/corba-event-service/src/**, modules/corba-event-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-360-notification-service-iiop-naming-exposure.md, docs/roadmap/tasks/g8-370-notification-service-native-smoke.md, README.md
Forbidden files: live peer execution, committed interop reports, peer artifacts, Docker layers, generated OMG compatibility APIs, broad Any/TypeCode expansion outside the supported structured-event subset, persistent storage, transaction integration, Native Image smoke, peer manifest metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, Java serialization metadata, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add descriptor-backed loopback IIOP and optional Naming exposure for the supported Notification Service subset, including channel/admin/proxy lookup, structured push/pull operations, filter and QoS rejection diagnostics, disconnect operations, malformed request diagnostics, and Naming-resolved NotificationChannel IORs. Keep evidence local and loopback-only.
Tests to add/update: Add loopback IIOP/Naming tests for supported operations, malformed object keys, unknown operations, invalid bodies, filter/QoS rejection over the wire, disconnect diagnostics, clean shutdown, and package documentation.
Documentation to update: Notification/Event READMEs as needed, services design, optional services conformance/review, interop matrix, roadmap index, README, and G8-370 status.
Commands to run: ./gradlew :modules:corba-notification-service:test :modules:corba-event-service:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: The supported Notification Service subset is reachable through local loopback IIOP/Naming with deterministic diagnostics; no Native Image smoke, peer metadata, live peer execution, or pass/fail peer claim is added; G8-370 is promoted after completion.
Completion evidence: Completed with descriptor-backed loopback IIOP/Naming exposure for the supported local Notification Service subset. Coverage exercises channel admin lookup, structured proxy creation, structured push/pull/try-pull delivery, disconnect operations, local filter and QoS rejection diagnostics, malformed request bodies, unknown object keys, unknown operations, stale proxy behavior after channel destroy, Naming-resolved NotificationChannel IORs, structured-event CDR round trips, and clean shutdown. Native Image smoke, peer metadata, live peer execution, persistence, transactions, generated OMG APIs, reflection metadata, dynamic proxies, serialization metadata, and pass/fail peer claims remain excluded.
Rollback notes: Revert loopback IIOP/Naming sources, tests, docs, and roadmap status together.
