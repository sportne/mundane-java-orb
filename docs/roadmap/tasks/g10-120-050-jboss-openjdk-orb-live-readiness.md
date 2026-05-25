# G10-120-050 JBoss OpenJDK ORB Live Readiness

Task ID: G10-120-050-JBOSS-OPENJDK-ORB-LIVE-READINESS
Status: complete
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-008, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0013
Specification references: CORBA-IF, CORBA-IOP, IDL-42
Target module: JBoss OpenJDK ORB peer readiness and interop harness
Allowed files: interop/**, modules/corba-interop-testkit/src/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-120-*.md, README.md
Forbidden files: production behavior changes, Gradle build logic changes, vendored peer source, committed peer binaries, raw live report outputs, optional service implementation, unapproved release version changes
Expected behavior: Task type: implementation. Add JBoss-specific peer readiness handling so server IOR publication is deterministic and host-visible before health and client lanes run. Prefer peer command and harness fixes over classification; classify as `profile-mismatch` only if deterministic command fixes prove the real JBoss ORB cannot provide a stable server lane.
Tests to add/update: Interop testkit coverage for JBoss readiness behavior, IOR publication diagnostics, and clean-room classification evidence if required.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interop plan, roadmap index, README, this task, and G10-120-060 status when complete.
Commands to run: ./gradlew :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; INTEROP_ARTIFACT_CACHE=/mnt/d/projects/mundane-java-orb/interop/work/artifact-cache ./interop/bin/interop-peer validate-gates --require-cache; ./gradlew validateDesignControlPack qualityGate; ./interop/bin/interop-peer run-direction-matrix --require-live basic-idl jboss-openjdk-orb; git diff --check
Acceptance criteria: `run-direction-matrix --require-live basic-idl jboss-openjdk-orb` passes or has maintainer-approved non-project classification with clean-room evidence.
Completion evidence: Completed on 2026-05-25. JBoss OpenJDK ORB server readiness now uses the shared Java peer smoke while skipping the extra GlassFish-style `com.sun.CORBA.transport.ORBListenSocket` listener property that caused the JBoss ORB to attempt a duplicate port bind. The approved rebuilt JBoss image passed `./interop/bin/interop-peer run-direction-matrix --require-live basic-idl jboss-openjdk-orb` with JVM and Native Image local lanes.
Rollback notes: Revert JBoss peer readiness wiring, related testkit coverage, and roadmap status updates.
