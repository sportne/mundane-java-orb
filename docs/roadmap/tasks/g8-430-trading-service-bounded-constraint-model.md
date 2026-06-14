# G8-430 Trading Service Bounded Constraint Model

Task ID: G8-430-TRADING-SERVICE-BOUNDED-CONSTRAINT-MODEL
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-010, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0020
Specification references: TRADE-10
Target module: modules/corba-trading-service
Allowed files: modules/corba-trading-service/src/**, modules/corba-trading-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-430-trading-service-bounded-constraint-model.md, docs/roadmap/tasks/g8-440-trading-service-local-query.md, README.md
Forbidden files: repository query integration, import/export behavior, IIOP/Naming exposure, Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, scripting engines, regex engines for constraint semantics, reflection metadata, dynamic proxies, Java serialization metadata, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add a bounded Trading Service constraint parser and evaluator over primitive property maps. Support boolean constants, property equality and inequality, numeric comparisons, `and`, `or`, `not`, and parentheses. Reject unsupported functions, wildcards, regex, arithmetic, unknown tokens, malformed syntax, expression length, token count, term count, and nesting-depth violations with deterministic diagnostics. Do not integrate offer repository query behavior yet.
Tests to add/update: Add unit tests for accepted boolean/property expressions, numeric comparisons, boolean composition, parentheses, unknown properties, type mismatches, malformed syntax, unsupported operators and functions, hostile oversized inputs, expression depth, token and term limits, deterministic parse diagnostics, and package documentation.
Documentation to update: Trading Service README, services design, optional services conformance/review, roadmap index, README, and G8-440 status.
Commands to run: ./gradlew :modules:corba-trading-service:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Trading Service has a closed-world bounded constraint parser/evaluator with deterministic diagnostics and no query integration, import/export, IIOP/Naming, Native Image, interop, or live peer claim; G8-440 is promoted after completion.
Rollback notes: Revert Trading Service constraint model sources, tests, docs, and roadmap status together.
