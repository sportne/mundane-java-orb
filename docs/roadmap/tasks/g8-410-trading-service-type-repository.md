# G8-410 Trading Service Type Repository

Task ID: G8-410-TRADING-SERVICE-TYPE-REPOSITORY
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-010, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0020
Specification references: TRADE-10
Target module: modules/corba-trading-service, modules/corba-services-core
Allowed files: modules/corba-trading-service/src/**, modules/corba-trading-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-410-trading-service-type-repository.md, docs/roadmap/tasks/g8-420-trading-service-offer-repository.md, README.md
Forbidden files: offer repository behavior, constraint parsing/evaluation, query behavior, import/export behavior, IIOP/Naming exposure, Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, durable persistence, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add an in-memory Trading Service type repository with bounded type names, primitive property definitions, create/update/delete/list/lookup behavior, immutable snapshots, and deterministic duplicate, missing, dependency, and limit diagnostics. Do not add offers, constraint evaluation, query behavior, import/export, IIOP/Naming, persistence, Native Image, or interop behavior.
Tests to add/update: Add unit tests for type registration, duplicate rejection, updates, deletion, missing-type diagnostics, dependent-type rejection or absence handling, bounded type/property limits, primitive property-kind validation, immutable snapshots, and package documentation.
Documentation to update: Trading Service and Services Core READMEs as needed, services design, optional services conformance/review, roadmap index, README, and G8-420 status.
Commands to run: ./gradlew :modules:corba-trading-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Trading Service exposes a bounded local type repository with deterministic diagnostics and no offer, query, persistence, IIOP/Naming, Native Image, interop, or live peer claim; G8-420 is promoted after completion.
Rollback notes: Revert Trading Service type repository sources, tests, docs, and roadmap status together.
