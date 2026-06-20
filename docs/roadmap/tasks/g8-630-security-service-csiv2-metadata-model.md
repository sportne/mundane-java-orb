# G8-630 Security Service CSIv2 Metadata Model

Task ID: G8-630-SECURITY-SERVICE-CSIV2-METADATA-MODEL
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-050, REQ-IIOP-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0022
Specification references: SEC-18, CORBA-IOP-SECURITY
Target module: modules/corba-security-service, modules/corba-services-core
Allowed files: modules/corba-security-service/src/**, modules/corba-security-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/architecture/cdr-giop-iiop.md, docs/conformance/optional-services-matrix.md, docs/requirements/service-requirements.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-630-security-service-csiv2-metadata-model.md, docs/roadmap/tasks/g8-640-security-service-local-policy-evaluation.md, README.md
Forbidden files: policy enforcement, IIOP runtime integration, Native Image smoke entrypoints, interop metadata, live peer execution, automatic TLS policy changes, generated OMG APIs, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add a bounded CSIv2 metadata model and project-owned deterministic encode/decode for the supported subset: mechanism identity, transport protection flags, identity token policy, target/client authentication metadata, size/count limits, malformed input diagnostics, and unsupported mechanism diagnostics. Do not add enforcement, IIOP runtime integration, automatic TLS policy changes, or live peer behavior.
Tests to add/update: Add unit tests for metadata creation, encode/decode round trips, malformed/oversized input rejection, unsupported mechanisms, target/client auth metadata bounds, deterministic diagnostics, immutable snapshots, and package documentation.
Documentation to update: Security Service and Services Core READMEs as needed, CDR/GIOP/IIOP architecture, services design, optional services conformance/review, service requirements, roadmap index, README, and G8-640 status.
Commands to run: ./gradlew :modules:corba-security-service:test :modules:corba-services-core:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Security Service exposes bounded CSIv2 metadata encode/decode with deterministic diagnostics and no policy enforcement, IIOP runtime integration, automatic TLS policy change, generated OMG API, or live peer claim; G8-640 is promoted after completion.
Rollback notes: Revert Security Service CSIv2 metadata sources, tests, docs, and roadmap status together.
