# G8-220 Event Service Local Delivery

Task ID: G8-220-EVENT-SERVICE-LOCAL-DELIVERY
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-020, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0018
Specification references: EVNT-12
Target module: modules/corba-event-service
Allowed files: modules/corba-event-service/src/**, modules/corba-event-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-220-event-service-local-delivery.md, docs/roadmap/tasks/g8-230-event-service-backpressure.md, README.md
Forbidden files: IIOP/Naming exposure, Native Image smoke entrypoints, interop harness changes, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated OMG compatibility APIs, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Implement in-JVM push supplier to push consumer delivery and pull consumer to pull supplier delivery through the G8-210 channel/proxy model. Keep all behavior local and non-persistent.
Tests to add/update: Add tests for connect/disconnect, push delivery, pull/try-pull, empty pull diagnostics, destroyed channel behavior, idempotent disconnect, and null/invalid payload rejection.
Documentation to update: Services design, optional services conformance/review, roadmap index, README, and module README.
Commands to run: ./gradlew :modules:corba-event-service:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Local push and pull delivery is deterministic and covered; G8-230 is promoted after completion; no network or peer behavior is added.
Rollback notes: Revert Event Service local delivery code, tests, docs, and roadmap status changes together.

Completion evidence: G8-220 adds local push supplier to push consumer delivery, pull consumer to pull supplier delivery, connect/disconnect behavior, empty pull diagnostics, null/hostile payload rejection, and destroyed-channel delivery diagnostics. G8-230 is promoted to ready-for-implementation.
