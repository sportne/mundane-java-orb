# G8-670 Security Service Native Image Smoke

Task ID: G8-670-SECURITY-SERVICE-NATIVE-SMOKE
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-050, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0022
Specification references: SEC-18, CORBA-IOP-SECURITY
Target module: modules/corba-security-service, modules/corba-native-image
Allowed files: modules/corba-security-service/src/**, modules/corba-security-service/README.md, modules/corba-native-image/src/**, modules/corba-native-image/build.gradle, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/native-image-matrix.md, docs/requirements/service-requirements.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-670-security-service-native-smoke.md, docs/roadmap/tasks/g8-680-security-service-interop-metadata.md, README.md
Forbidden files: interop metadata, live peer execution, committed live interop reports, automatic TLS policy changes, global JVM security-manager integration, enterprise identity management, Native Image binary commits, reflection metadata, dynamic proxy metadata, Java serialization metadata, service-loader discovery, scripting engines, runtime bytecode generation, process execution in production sources, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add Native Image smoke coverage for credential rejection, trust validation, policy rejection, CSIv2 metadata encode/decode, local policy evaluation, audit redaction, IIOP boundary behavior, and clean shutdown. Extend source-level audits to Security Service production sources without forbidden metadata or dynamic runtime mechanisms.
Tests to add/update: Add native smoke entrypoint and JVM parity test coverage, update Native Image target lists, and extend source-level audits for Security Service production sources.
Documentation to update: Security Service README, services design, optional services conformance/review, Native Image matrix, service requirements, roadmap index, README, and G8-680 status.
Commands to run: ./gradlew :modules:corba-security-service:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Security Service has deterministic Native Image smoke coverage and source audits with no reflection metadata, dynamic proxies, Java serialization metadata, service-loader discovery, scripting, bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`, automatic TLS policy change, or live peer claim; G8-680 is promoted after completion.
Rollback notes: Revert Security Service Native Image smoke sources, tests, docs, and roadmap status together.
