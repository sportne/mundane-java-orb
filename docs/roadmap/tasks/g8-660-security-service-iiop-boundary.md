# G8-660 Security Service IIOP Boundary

Task ID: G8-660-SECURITY-SERVICE-IIOP-BOUNDARY
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-050, REQ-IIOP-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0022
Specification references: SEC-18, CORBA-IOP-SECURITY
Target module: modules/corba-security-service, modules/corba-services-core, modules/corba-iiop
Allowed files: modules/corba-security-service/src/**, modules/corba-security-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, modules/corba-iiop/src/**, modules/corba-iiop/README.md, docs/architecture/services-design.md, docs/architecture/cdr-giop-iiop.md, docs/conformance/optional-services-matrix.md, docs/requirements/service-requirements.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-660-security-service-iiop-boundary.md, docs/roadmap/tasks/g8-670-security-service-native-smoke.md, README.md
Forbidden files: Native Image smoke entrypoints, interop metadata, live peer execution, committed live interop reports, automatic TLS/mTLS policy changes, global JVM security-manager integration, generated OMG APIs, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add a descriptor-backed loopback IIOP boundary for the supported local security subset, including bounded CSIv2 service-context/tagged-component handling, local policy evaluation on loopback requests, malformed context diagnostics, and clean shutdown. Do not change TLS/mTLS policy automatically or claim live peer behavior.
Tests to add/update: Add unit tests for loopback security context handling, tagged component/service context encode/decode, local policy allow/deny/challenge outcomes through the boundary, malformed context diagnostics, audit redaction, clean shutdown, and package documentation.
Documentation to update: Security Service, Services Core, and IIOP docs as needed, CDR/GIOP/IIOP architecture, services design, optional services conformance/review, interop matrix, service requirements, roadmap index, README, and G8-670 status.
Commands to run: ./gradlew :modules:corba-security-service:test :modules:corba-services-core:test :modules:corba-iiop:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Security Service exposes local loopback IIOP boundary behavior with deterministic diagnostics and no automatic TLS policy changes, global JVM security-manager integration, generated OMG APIs, or live peer claim; G8-670 is promoted after completion.
Rollback notes: Revert Security Service IIOP boundary sources, tests, docs, and roadmap status together.
