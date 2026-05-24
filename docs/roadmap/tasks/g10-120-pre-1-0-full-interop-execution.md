# G10-120 Pre-1.0 Full Interop Execution

Task ID: G10-120-PRE-1.0-FULL-INTEROP-EXECUTION
Status: ready-for-implementation
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-002
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0013
Specification references: CORBA-IF, CORBA-IOP, IDL-42, I2JAV-13, JAV2I-14, NAM-13
Target module: interop verification and documentation
Allowed files: interop/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/conformance/*.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-120-pre-1-0-full-interop-execution.md, README.md
Forbidden files: production behavior changes, test harness implementation changes, Gradle build logic changes, vendored peer source, committed peer binaries, optional service implementation, unapproved release version changes
Expected behavior: Task type: verification-only. Execute the complete non-optional pre-1.0 live peer matrix after all earlier G10 tasks complete and approved live prerequisites are present, including our JVM/native clients and servers, peer clients and servers, Naming, IOR/object URL, GIOP/IIOP, IDL-to-Java, DynamicAny/DII/DSI, Portable Interceptor, and RMI-IIOP scenarios.
Tests to add/update: No product tests unless report parsing or summary validation gaps are found; use the completed harness to generate clean-room structured evidence.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interop plan, conformance matrices, roadmap index, README ready-task status, and this task.
Commands to run: ./gradlew test; ./gradlew qualityGate; ./gradlew validateDesignControlPack; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer validate-gates --require-cache; ./interop/bin/interop-peer run-scenario --require-live <scenario> all; git diff --check
Acceptance criteria: This task remains blocked until an approved external artifact cache, digest-pinned Java and native base images, prepared peer images with real peer command entrypoints, Docker/Podman access, and Native Image lane inputs are present; every non-optional live scenario either passes or is classified with maintainer-approved evidence as peer-bug, spec-ambiguity, or profile-mismatch; any our-bug or unresolved infrastructure-failure blocks 1.0.0; optional services remain explicitly deferred.
Current execution note: On 2026-05-24, local prerequisite validation failed before live peer execution because `INTEROP_ARTIFACT_CACHE` was unset and no approved cache entries were available. `run-scenario --require-live basic-idl all` produced structured `infrastructure-failure` reports for the server lane of JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, and ACE/TAO. No pass/fail compatibility evidence was recorded.
Rollback notes: Revert generated evidence documentation and roadmap status updates; do not revert completed implementation tasks.
