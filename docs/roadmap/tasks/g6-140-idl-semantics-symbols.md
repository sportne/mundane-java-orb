# G6-140 IDL Semantics and Symbols

Task ID: G6-140-IDL-SEMANTICS-SYMBOLS
Status: complete
Gate: G6 IDL compiler vertical slice
Requirement IDs: REQ-IDL-002, REQ-IDL-003, REQ-DOC-001, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0004, ADR-0005
Specification references: IDL-42-SCOPING, IDL-42-GRAMMAR, IDL-42-PROFILES
Target module: modules/corba-idl-semantics
Allowed files: modules/corba-idl-semantics/build.gradle, modules/corba-idl-semantics/src/main/**, modules/corba-idl-semantics/src/test/**, modules/corba-idl-semantics/src/nativeSmoke/**, modules/corba-idl-semantics/README.md, modules/corba-idl-ast/src/main/**, docs/architecture/idl-compiler-architecture.md, docs/conformance/idl-4.2-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-140-idl-semantics-symbols.md, docs/roadmap/tasks/g6-150-idlj-validate-cli.md, docs/roadmap/tasks/g6-610-poa-policy-matrix.md, README.md
Forbidden files: Java code generation, ORB runtime, protocol encoding
Expected behavior: Task type: implementation. Resolve names, build symbol tables, evaluate simple constants, and emit normalized semantic models for the minimal parser subset.
Tests to add/update: Unit/spec/negative tests for duplicate names, nested scopes, forward references selected for the first profile, deterministic model output, and Native Image smoke coverage.
Documentation to update: Semantic package docs, module README, IDL compiler architecture, IDL conformance matrix, native-image matrix, and roadmap status.
Commands to run: ./gradlew :modules:corba-idl-semantics:test; ./gradlew :modules:corba-idl-semantics:nativeIdlSemanticsSmoke; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Semantic model can drive validation and later mapping without using reflection or dynamic scanning, and the parser-to-semantics smoke path builds and runs as a GraalVM Native Image.
Rollback notes: Revert semantic model implementation, tests, and docs together.
