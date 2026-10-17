# October 2026 maintenance

Task ID: MAINT-2026-10
Status: in-progress
Gate: Maintainer-requested build infrastructure maintenance
Requirement IDs: REQ-BUILD-004, REQ-BUILD-005, REQ-NATIVE-001, REQ-NATIVE-005
ADR IDs: ADR-0010
Specification references: docs/verification/native-image-matrix.md
Target module: CI workflows and existing native verification wiring.
Allowed files: .github/workflows/**, docs/verification/native-image-matrix.md, docs/build/maintenance-2026-10.md
Forbidden files: CORBA runtime, protocol, IDL, service behavior, standards, dependency locks and generated code.
Expected behavior: Task type: verification-only. Refresh deprecated action runtimes and make the native workflow execute its existing binary smoke aggregate.
Tests to add/update: Existing JVM gates and aggregate native smoke binaries.
Documentation to update: Native Image matrix and this maintenance scope.
Commands to run: ./gradlew validateDesignControlPack qualityGate --console=plain; ./gradlew :modules:corba-native-image:nativeImageBinariesSmoke --console=plain; git diff --check
Acceptance criteria: Existing product behavior and gates are preserved; missing native-image fails explicitly in the native workflow. Record local and hosted results in the pull request.
Rollback notes: Revert workflow and corresponding verification documentation together.

Authorized by the owner's October 17 maintenance request. This is not acceptance
of a new runtime feature, interoperability claim, or unrelated roadmap task.
