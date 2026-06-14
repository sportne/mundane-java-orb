# G8-420 Trading Service Offer Repository

Task ID: G8-420-TRADING-SERVICE-OFFER-REPOSITORY
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-010, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0020
Specification references: TRADE-10
Target module: modules/corba-trading-service, modules/corba-services-core
Allowed files: modules/corba-trading-service/src/**, modules/corba-trading-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-420-trading-service-offer-repository.md, docs/roadmap/tasks/g8-430-trading-service-bounded-constraint-model.md, README.md
Forbidden files: constraint parsing/evaluation, query behavior, import/export behavior, IIOP/Naming exposure, Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, durable offer persistence, transaction integration, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add an in-memory offer repository whose offers reference registered service types, carry bounded primitive property values, support create/update/withdraw/lookup/list-by-type, and reject missing type, duplicate offer, missing offer, property mismatch, unsupported value, and configured limit violations with deterministic diagnostics. Do not add constraint queries, import/export, IIOP/Naming, persistence, Native Image, or interop behavior.
Tests to add/update: Add unit tests for offer registration, updates, withdrawal, lookup, list-by-type ordering, type compatibility, missing type, duplicate offer, missing offer, property mismatch, value limits, offer count limits, and immutable snapshots.
Documentation to update: Trading Service and Services Core READMEs as needed, services design, optional services conformance/review, roadmap index, README, and G8-430 status.
Commands to run: ./gradlew :modules:corba-trading-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Trading Service exposes bounded in-memory offer CRUD over registered service types with deterministic diagnostics and no query, import/export, persistence, IIOP/Naming, Native Image, interop, or live peer claim; G8-430 is promoted after completion.
Rollback notes: Revert Trading Service offer repository sources, tests, docs, and roadmap status together.
