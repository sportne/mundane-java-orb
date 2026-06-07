# G8-210 Event Service Channel Model

Task ID: G8-210-EVENT-SERVICE-CHANNEL-MODEL
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-020, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0018
Specification references: EVNT-12
Target module: modules/corba-event-service
Allowed files: modules/corba-event-service/src/**, modules/corba-event-service/build.gradle, modules/corba-event-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-210-event-service-channel-model.md, docs/roadmap/tasks/g8-220-event-service-local-delivery.md, README.md
Forbidden files: IIOP/Naming exposure, Native Image smoke entrypoints, interop harness changes, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated OMG compatibility APIs, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add local Event Service channel lifecycle model, service options, diagnostics, supplier/consumer admin surfaces, and proxy handles using existing project `AnyValue<?>` payloads. Do not implement delivery behavior beyond lifecycle validation in this slice.
Tests to add/update: Add focused `modules:corba-event-service` unit tests for option bounds, channel creation/destruction, duplicate/destroyed lifecycle diagnostics, and admin/proxy handle ownership.
Documentation to update: Services design, optional services conformance/review, roadmap index, README, and module README.
Commands to run: ./gradlew :modules:corba-event-service:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Local channel lifecycle APIs are explicit, bounded, covered by unit tests, and Native Image compatible; G8-220 is promoted after completion; no runtime network, interop, persistence, or live peer behavior is added.
Completion evidence: Added the local `io.github.mundanej.mjo.event` channel lifecycle model with bounded options, stable diagnostics, supplier/consumer admin surfaces, proxy handles, and callback interfaces typed with project `AnyValue<?>` payloads. Unit tests cover option bounds, channel creation/destruction, duplicate destroyed-state diagnostics, service shutdown, channel limit rejection, and admin/proxy ownership. G8-220 is promoted for local push/pull delivery.
Rollback notes: Revert Event Service model code, tests, docs, and roadmap status changes together.
