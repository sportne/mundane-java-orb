# G8-450 Trading Service Import Export Boundary

Task ID: G8-450-TRADING-SERVICE-IMPORT-EXPORT-BOUNDARY
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-010, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0020
Specification references: TRADE-10
Target module: modules/corba-trading-service
Allowed files: modules/corba-trading-service/src/**, modules/corba-trading-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-450-trading-service-import-export-boundary.md, docs/roadmap/tasks/g8-460-trading-service-iiop-naming-exposure.md, README.md
Forbidden files: remote federation execution, remote trader graph traversal, IIOP/Naming exposure, Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, durable offer persistence, transaction integration, reflection metadata, scripting engines, dynamic proxies, Java serialization metadata, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add bounded import/export boundary metadata and validation for future Trading Service federation without executing remote federation. Validate link names, direction, fan-out limits, duplicate links, missing links, disabled remote federation, and query-time remote behavior diagnostics deterministically. Keep local query local-only.
Tests to add/update: Add unit tests for import/export link registration, duplicate rejection, missing link diagnostics, name and fan-out limits, disabled remote federation diagnostics, local-query isolation from import/export metadata, and package documentation.
Documentation to update: Trading Service README, services design, optional services conformance/review, roadmap index, README, and G8-460 status.
Commands to run: ./gradlew :modules:corba-trading-service:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Trading Service records and validates a bounded import/export boundary without remote federation, persistence, IIOP/Naming, Native Image, interop, or live peer claim; G8-460 is promoted after completion.
Rollback notes: Revert Trading Service import/export boundary sources, tests, docs, and roadmap status together.
