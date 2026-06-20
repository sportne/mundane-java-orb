# G8-650 Security Service Audit Failure Disclosure

Task ID: G8-650-SECURITY-SERVICE-AUDIT-FAILURE-DISCLOSURE
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-050, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0022
Specification references: SEC-18, CORBA-IOP-SECURITY
Target module: modules/corba-security-service, modules/corba-services-core
Allowed files: modules/corba-security-service/src/**, modules/corba-security-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/requirements/service-requirements.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-650-security-service-audit-failure-disclosure.md, docs/roadmap/tasks/g8-660-security-service-iiop-boundary.md, README.md
Forbidden files: IIOP integration, Native Image smoke entrypoints, interop metadata, live peer execution, raw credential material in diagnostics, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add an audit and failure disclosure model with redacted audit events, bounded event fields, stable denial reasons, and explicit secret redaction. Ensure raw credential material is not exposed through messages, `toString`, reports, or diagnostics. Do not add IIOP integration or live peer behavior.
Tests to add/update: Add unit tests for audit event creation, denial reason mapping, bounded fields, secret redaction, `toString` redaction, exception message redaction, deterministic ordering, and package documentation.
Documentation to update: Security Service and Services Core READMEs as needed, services design, optional services conformance/review, service requirements, roadmap index, README, and G8-660 status.
Commands to run: ./gradlew :modules:corba-security-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Security Service exposes redacted audit/failure disclosure with deterministic diagnostics and no raw credential leakage, IIOP integration, or live peer claim; G8-660 is promoted after completion.
Rollback notes: Revert Security Service audit/failure disclosure sources, tests, docs, and roadmap status together.
