# G6-150 idlj Validate CLI

Task ID: G6-150-IDLJ-VALIDATE-CLI
Status: draft
Gate: G6 IDL compiler vertical slice
Requirement IDs: REQ-IDLJ-001, REQ-IDL-001, REQ-IDL-002, REQ-IDL-003, REQ-DOC-001, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0004, ADR-0005, ADR-0009
Specification references: I2JAV-13, IDL-42
Target module: modules/corba-idlj-cli
Allowed files: modules/corba-idlj-cli/src/main/**, modules/corba-idlj-cli/src/test/**, docs/architecture/idl-compiler-architecture.md
Forbidden files: Java source generation, CDR codec generation, ORB runtime, protocol behavior
Expected behavior: Task type: implementation. Add an idlj-like CLI that validates IDL files and reports stable diagnostics without emitting generated code.
Tests to add/update: CLI unit tests for success, diagnostic exit codes, include path handling, and deterministic output.
Documentation to update: CLI package docs and IDL compiler architecture.
Commands to run: ./gradlew :modules:corba-idlj-cli:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Users can run validation over a small IDL corpus with stable exit behavior.
Rollback notes: Revert CLI validation implementation, tests, and docs together.

