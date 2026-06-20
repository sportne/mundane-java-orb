# G8-520 Transaction Service Timeout Policy

Task ID: G8-520-TRANSACTION-SERVICE-TIMEOUT-POLICY
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-040, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0021
Specification references: TRANS-14
Target module: modules/corba-transaction-service, modules/corba-services-core
Allowed files: modules/corba-transaction-service/src/**, modules/corba-transaction-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-520-transaction-service-timeout-policy.md, docs/roadmap/tasks/g8-530-transaction-service-local-state-transitions.md, README.md
Forbidden files: completion/state-transition callbacks beyond timeout marking, propagation metadata, recovery boundary, IIOP request-context integration, Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, durable recovery logs, ambient scheduler threads, XA integration, Security Service integration, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add explicit timeout policy for local transactions with bounded timeout values, caller-injected clock, begin-time and deadline metadata, timeout validation, deterministic expired and stale transaction diagnostics, and no ambient schedulers or durable logs. Do not add completion callbacks beyond timeout state/diagnostic support, propagation, recovery, IIOP, Native Image, interop, XA, Security Service, distributed two-phase commit, or live peer behavior.
Tests to add/update: Add unit tests for default and caller-configured timeout policy, invalid timeout rejection, deadline calculation through an injected clock, expired transaction diagnostics, stale transaction diagnostics, no ambient scheduler behavior, and immutable timeout metadata.
Documentation to update: Transaction Service README, services design, optional services conformance/review, roadmap index, README, and G8-530 status.
Commands to run: ./gradlew :modules:corba-transaction-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Transaction Service exposes bounded caller-configured timeout policy and deterministic timeout diagnostics without ambient scheduling, durable logs, propagation, IIOP, Native Image, interop, or live peer claims; G8-530 is promoted after completion.
Rollback notes: Revert Transaction Service timeout policy sources, tests, docs, and roadmap status together.
