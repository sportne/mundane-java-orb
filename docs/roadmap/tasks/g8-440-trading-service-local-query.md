# G8-440 Trading Service Local Query

Task ID: G8-440-TRADING-SERVICE-LOCAL-QUERY
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-010, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0020
Specification references: TRADE-10
Target module: modules/corba-trading-service
Allowed files: modules/corba-trading-service/src/**, modules/corba-trading-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-440-trading-service-local-query.md, docs/roadmap/tasks/g8-450-trading-service-import-export-boundary.md, README.md
Forbidden files: import/export federation behavior, remote trader graphs, IIOP/Naming exposure, Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, durable offer persistence, transaction integration, reflection metadata, scripting engines, dynamic proxies, Java serialization metadata, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add local type-scoped query over the in-memory offer repository using the bounded constraint evaluator. Results must be deterministic by offer id, obey caller or configured result limits, bound query cost, and preserve clear diagnostics for unknown type, malformed constraint, unknown property, type mismatch, and configured limit violations. Do not add import/export federation, IIOP/Naming, persistence, Native Image, or interop behavior.
Tests to add/update: Add unit tests for exact and comparison matches, empty results, deterministic ordering, max-result truncation or rejection as specified by the implementation diagnostics, unknown type, malformed constraint, unknown property, type mismatch, query cost limits, and package documentation.
Documentation to update: Trading Service README, services design, optional services conformance/review, roadmap index, README, and G8-450 status.
Commands to run: ./gradlew :modules:corba-trading-service:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Trading Service supports bounded local offer query with deterministic results and diagnostics and no import/export, persistence, IIOP/Naming, Native Image, interop, or live peer claim; G8-450 is promoted after completion.
Rollback notes: Revert Trading Service local query sources, tests, docs, and roadmap status together.
