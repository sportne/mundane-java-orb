# G6-830 Real Peer Interop Reports

Task ID: G6-830-REAL-PEER-INTEROP-REPORTS
Gate: G6 interop verification
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0006, ADR-0010
Specification references: CORBA-IOP-ARCH, CORBA-IOP-GIOP, CORBA-IOP-IIOP, CORBA-IOP-IOR
Target module: modules/corba-interop-testkit and interop
Allowed files: modules/corba-interop-testkit/src/**, interop/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md
Forbidden files: vendored peer source, committed peer binaries, source-derived reference implementation logic
Expected behavior: Task type: implementation. Run approved peer containers, execute JVM/native client/server scenarios, and produce structured clean-room interop reports.
Tests to add/update: Interop-tagged tests for selected scenario groups and peer directions.
Documentation to update: Interop matrix, reference behavior capture records, and peer READMEs.
Commands to run: ./gradlew :modules:corba-interop-testkit:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Interop failures produce structured reports and no peer artifacts are committed.
Rollback notes: Revert interop-testkit changes, scripts, reports schema/docs, and manifest updates together.

