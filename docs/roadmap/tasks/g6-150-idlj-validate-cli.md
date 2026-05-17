# G6-150 idlj Validate CLI

Task ID: G6-150-IDLJ-VALIDATE-CLI
Status: complete
Gate: G6 IDL compiler vertical slice
Requirement IDs: REQ-IDLJ-001, REQ-IDL-001, REQ-IDL-002, REQ-IDL-003, REQ-DOC-001, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0004, ADR-0005, ADR-0009
Specification references: I2JAV-13, IDL-42
Target module: modules/corba-idlj-cli
Allowed files: modules/corba-idlj-cli/build.gradle, modules/corba-idlj-cli/src/main/**, modules/corba-idlj-cli/src/test/**, modules/corba-idlj-cli/src/nativeSmoke/**, modules/corba-idlj-cli/README.md, modules/corba-idl-parser/src/main/java/io/github/mundanej/mjo/idl/parser/IdlParser.java, docs/architecture/idl-compiler-architecture.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-150-idlj-validate-cli.md, docs/roadmap/tasks/g6-160-idl-java-minimal-generation.md, README.md
Forbidden files: Java source generation, CDR codec generation, ORB runtime, protocol behavior
Expected behavior: Task type: implementation. Add an idlj-like CLI that validates IDL files and reports stable diagnostics without emitting generated code.
Tests to add/update: CLI unit tests for success, diagnostic exit codes, include path handling, deterministic output, and Native Image smoke coverage.
Documentation to update: CLI package docs, module README, IDL compiler architecture, native-image matrix, and roadmap status.
Commands to run: ./gradlew :modules:corba-idlj-cli:test; ./gradlew :modules:corba-idlj-cli:nativeIdljValidateSmoke; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Users can run validation over a small IDL corpus with stable exit behavior, and the validate command builds and runs as a compact GraalVM Native Image smoke executable.
Rollback notes: Revert CLI validation implementation, tests, and docs together.
