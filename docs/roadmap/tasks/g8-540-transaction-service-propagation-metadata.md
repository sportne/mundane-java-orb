# G8-540 Transaction Service Propagation Metadata

Task ID: G8-540-TRANSACTION-SERVICE-PROPAGATION-METADATA
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-040, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0021
Specification references: TRANS-14
Target module: modules/corba-transaction-service, modules/corba-services-core
Allowed files: modules/corba-transaction-service/src/**, modules/corba-transaction-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-540-transaction-service-propagation-metadata.md, docs/roadmap/tasks/g8-550-transaction-service-recovery-boundary.md, README.md
Forbidden files: IIOP request-context integration, recovery boundary beyond explicit metadata notes, Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, durable recovery logs, XA integration, Security Service integration, peer distributed two-phase commit claims, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add bounded propagation metadata with immutable local propagation contexts, deterministic encode/decode within project-owned models, validation for stale, expired, malformed, unknown, and oversized contexts, and parent/child or imported-context identity only when needed for local diagnostics. Do not integrate with IIOP request contexts, recovery logs, Native Image, interop, XA, Security Service, peer distributed two-phase commit, or live peer behavior.
Tests to add/update: Add unit tests for context creation, deterministic encode/decode, malformed input rejection, stale/unknown transaction diagnostics, expired context diagnostics through the timeout policy, size and field-count limits, immutable context snapshots, and absence of Java serialization.
Documentation to update: Transaction Service README, services design, optional services conformance/review, roadmap index, README, and G8-550 status.
Commands to run: ./gradlew :modules:corba-transaction-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Transaction Service exposes bounded local propagation metadata and deterministic diagnostics without IIOP request-context integration, recovery logs, Native Image, interop, distributed peer two-phase commit, or live peer claims; G8-550 is promoted after completion.
Completion evidence: Added immutable local propagation contexts, deterministic bounded text encode/decode, coordinator export/validation, unknown/stale/expired/malformed/oversized diagnostics, and tests covering context immutability, codec field limits, stale generation checks, timeout-policy expiry, and absence of Java serialization. G8-550 is promoted to `ready-for-implementation`.
Rollback notes: Revert Transaction Service propagation metadata sources, tests, docs, and roadmap status together.
