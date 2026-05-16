# G6-910 Native Image Binaries

Task ID: G6-910-NATIVE-IMAGE-BINARIES
Status: draft
Gate: G6 native-image hardening
Requirement IDs: REQ-NATIVE-001, REQ-NATIVE-002, REQ-NATIVE-003, REQ-NATIVE-004, REQ-NATIVE-005, REQ-BUILD-005
ADR IDs: ADR-0010
Specification references: Feature-specific native tasks cite the relevant CORBA, IDL, or Naming references.
Target module: modules/corba-native-image
Allowed files: modules/corba-native-image/src/**, build-logic/**, docs/verification/native-image-matrix.md, docs/architecture/native-image-design.md
Forbidden files: reflection metadata generated without review, runtime bytecode generation, broad classpath scanning
Expected behavior: Task type: verification-only. Build and verify Native Image binaries for idlj, generated clients/servers, naming server, diagnostics, and selected interop fixtures.
Tests to add/update: Native-image tagged smoke, integration, startup/shutdown, class-initialization, and metadata audit tests.
Documentation to update: Native Image matrix and module initialization policy notes.
Commands to run: ./gradlew validateDesignControlPack qualityGate; native-image validation command TBD by task update; git diff --check
Acceptance criteria: Native Image behavior is deterministic and metadata changes are reviewed as source.
Rollback notes: Revert native-image validation configuration, tests, and docs together.
