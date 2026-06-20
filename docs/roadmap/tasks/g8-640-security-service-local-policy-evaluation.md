# G8-640 Security Service Local Policy Evaluation

Task ID: G8-640-SECURITY-SERVICE-LOCAL-POLICY-EVALUATION
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-050, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0022
Specification references: SEC-18, CORBA-IOP-SECURITY
Target module: modules/corba-security-service, modules/corba-services-core
Allowed files: modules/corba-security-service/src/**, modules/corba-security-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/requirements/service-requirements.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-640-security-service-local-policy-evaluation.md, docs/roadmap/tasks/g8-650-security-service-audit-failure-disclosure.md, README.md
Forbidden files: IIOP integration, audit event model beyond evaluation reason strings, Native Image smoke entrypoints, interop metadata, live peer execution, automatic TLS policy changes, global JVM security-manager integration, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add local policy evaluation using the credential/trust, policy, and CSIv2 metadata models. Return deterministic allow, deny, and challenge decisions with stable diagnostics for missing credentials, untrusted credentials, expired credentials, unsupported delegation, insufficient transport protection, malformed metadata, and policy conflicts. Do not add IIOP integration or live peer behavior.
Tests to add/update: Add unit tests for allow, deny, and challenge decisions, missing/expired/untrusted credentials, unsupported delegation, insufficient transport protection, malformed metadata, policy conflict diagnostics, deterministic reason ordering, and package documentation.
Documentation to update: Security Service and Services Core READMEs as needed, services design, optional services conformance/review, service requirements, roadmap index, README, and G8-650 status.
Commands to run: ./gradlew :modules:corba-security-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Security Service evaluates local policy deterministically with bounded diagnostics and no IIOP integration, automatic TLS policy change, global JVM security-manager integration, or live peer claim; G8-650 is promoted after completion.
Rollback notes: Revert Security Service local policy evaluation sources, tests, docs, and roadmap status together.
