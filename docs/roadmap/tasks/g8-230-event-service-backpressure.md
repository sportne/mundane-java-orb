# G8-230 Event Service Backpressure

Task ID: G8-230-EVENT-SERVICE-BACKPRESSURE
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-020, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0018
Specification references: EVNT-12
Target module: modules/corba-event-service
Allowed files: modules/corba-event-service/src/**, modules/corba-event-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-230-event-service-backpressure.md, docs/roadmap/tasks/g8-240-event-service-iiop-naming-exposure.md, README.md
Forbidden files: IIOP/Naming exposure, Native Image smoke entrypoints, interop harness changes, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated OMG compatibility APIs, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Enforce bounded max channels, suppliers, consumers, pending events, failed-consumer behavior, and stale proxy diagnostics through `EventServiceOptions`.
Tests to add/update: Add tests for channel/admin/proxy limits, queue overflow, slow or failing consumers, disconnected suppliers/consumers, stale proxies, and deterministic diagnostics.
Documentation to update: Services design, optional services conformance/review, roadmap index, README, and module README.
Commands to run: ./gradlew :modules:corba-event-service:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Event Service resource limits and backpressure diagnostics are deterministic and covered; G8-240 is promoted after completion; no network or peer behavior is added.
Rollback notes: Revert Event Service backpressure code, tests, docs, and roadmap status changes together.
