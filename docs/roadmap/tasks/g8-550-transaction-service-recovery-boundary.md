# G8-550 Transaction Service Recovery Boundary

Task ID: G8-550-TRANSACTION-SERVICE-RECOVERY-BOUNDARY
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-040, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0021
Specification references: TRANS-14
Target module: modules/corba-transaction-service, modules/corba-services-core
Allowed files: modules/corba-transaction-service/src/**, modules/corba-transaction-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-550-transaction-service-recovery-boundary.md, docs/roadmap/tasks/g8-560-transaction-service-iiop-request-context-boundary.md, README.md
Forbidden files: durable transaction logs, retention policy, log replay, migrations, IIOP request-context integration, Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, XA integration, Security Service integration, peer distributed two-phase commit claims, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add the local recovery boundary only: document recovery assumptions in code and docs, expose explicit disabled durable-recovery policy or diagnostics as needed, and keep transaction behavior deterministic when durable recovery is disabled. Do not add durable transaction logs, retention policy, log replay, migrations, IIOP, Native Image, interop, XA, Security Service, peer distributed two-phase commit, or live peer behavior.
Tests to add/update: Add unit tests for disabled durable-recovery diagnostics, recovery policy defaults, rejection of unapproved durable recovery requests, deterministic behavior after terminal local states, and documentation/package coverage for the recovery boundary.
Documentation to update: Transaction Service README, services design, optional services conformance/review, roadmap index, README, and G8-560 status.
Commands to run: ./gradlew :modules:corba-transaction-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Transaction Service records an explicit recovery boundary and deterministic disabled-recovery diagnostics without durable logs, IIOP, Native Image, interop, distributed peer two-phase commit, or live peer claims; G8-560 is promoted after completion.
Rollback notes: Revert Transaction Service recovery boundary sources, tests, docs, and roadmap status together.
