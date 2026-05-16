# G6-D30 Legal and Public Release

Task ID: G6-D30-LEGAL-PUBLIC-RELEASE
Status: human-gate-blocked
Gate: Deferred human approval gate
Requirement IDs: REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-OFFLINE-005
ADR IDs: ADR-0006, ADR-0011, ADR-0012
Specification references: Legal/license release task; no direct OMG clause.
Target module: repository release documentation
Allowed files: docs/**, interop/peers/*/peer.yaml, release metadata TBD by legal approval
Forbidden files: public license declaration, release publication, vendored peer source, vendored peer binaries before approval
Expected behavior: Task type: human-gate-blocked. Record license, dependency policy, reference peer, and public-release approvals before publishing artifacts.
Tests to add/update: Release validation commands only after human approval.
Documentation to update: License/release notes, peer license review notes, and dependency policy records.
Commands to run: ./gradlew validateDesignControlPack qualityGate; release validation commands TBD by human approval record; git diff --check
Acceptance criteria: No public release action occurs without explicit legal and maintainer approval.
Rollback notes: Revert legal/release planning docs together.
