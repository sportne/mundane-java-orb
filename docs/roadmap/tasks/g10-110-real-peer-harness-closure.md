# G10-110 Real Peer Harness Closure

Task ID: G10-110-REAL-PEER-HARNESS-CLOSURE
Status: blocked
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010
Specification references: CORBA-IOP-ARCH, CORBA-IOP-GIOP, CORBA-IOP-IIOP, CORBA-IOP-IOR, NAM-13, JAV2I-14-RMI-IDL
Target module: interop and modules/corba-interop-testkit
Allowed files: interop/**, modules/corba-interop-testkit/src/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-110-real-peer-harness-closure.md, docs/roadmap/tasks/g10-120-pre-1-0-full-interop-execution.md, README.md
Forbidden files: vendored peer source, committed peer binaries, committed live report outputs, unapproved external downloads, optional service implementation, production ORB behavior changes outside harness integration points
Expected behavior: Task type: implementation. Replace scaffold-only peer containers with real black-box launchers for JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, and ACE/TAO; add opt-in approved cache preparation, digest-pinned base-image handling, deterministic health checks, IOR exchange, scenario orchestration, report aggregation, and clean-room failure classification.
Tests to add/update: Interop CLI, fixture peer, cache preparation dry-run and opt-in paths, missing-prerequisite reports, peer command failure, report summary, clean-room boundary, and scenario selection tests.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interop plan, peer READMEs, roadmap index, README ready-task status, this task, and G10-120 status when complete.
Commands to run: ./gradlew :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer validate-gates; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: This task remains blocked until G10-100 is complete; harness can execute real peer scenarios only when approved cache, digest-pinned base images, and Docker/Podman are present; no peer source/binaries or live outputs are committed.
Rollback notes: Revert interop harness, testkit, documentation, and roadmap changes together.
