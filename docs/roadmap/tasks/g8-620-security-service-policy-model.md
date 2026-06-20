# G8-620 Security Service Policy Model

Task ID: G8-620-SECURITY-SERVICE-POLICY-MODEL
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-050, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0022
Specification references: SEC-18, CORBA-IOP-SECURITY
Target module: modules/corba-security-service, modules/corba-services-core
Allowed files: modules/corba-security-service/src/**, modules/corba-security-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/requirements/service-requirements.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-620-security-service-policy-model.md, docs/roadmap/tasks/g8-630-security-service-csiv2-metadata-model.md, README.md
Forbidden files: policy enforcement, CSIv2 encoding, IIOP integration, TLS reconfiguration, Native Image smoke entrypoints, interop metadata, live peer execution, global JVM security-manager integration, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add bounded Security Service policy objects and validation for authentication required/optional, trust required, transport protection requirement, identity assertion mode, delegation disabled/unsupported diagnostics, audit level, duplicate/conflicting policy rejection, and deterministic policy snapshots. Do not add policy enforcement, CSIv2 encoding, IIOP integration, TLS reconfiguration, or live peer behavior.
Tests to add/update: Add unit tests for default policies, explicit authentication/trust/transport/audit policies, duplicate and conflicting policy diagnostics, unsupported delegation diagnostics, malformed values, configured limits, immutable snapshots, and package documentation.
Documentation to update: Security Service and Services Core READMEs as needed, services design, optional services conformance/review, service requirements, roadmap index, README, and G8-630 status.
Commands to run: ./gradlew :modules:corba-security-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Security Service exposes bounded policy validation with deterministic diagnostics and no enforcement, CSIv2 encoding, IIOP integration, TLS reconfiguration, or live peer claim; G8-630 is promoted after completion.
Rollback notes: Revert Security Service policy sources, tests, docs, and roadmap status together.
