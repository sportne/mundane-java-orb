# G6-120 IDL Preprocessor and Includes

Task ID: G6-120-IDL-PREPROCESSOR-INCLUDES
Status: ready-for-implementation
Gate: G6 IDL compiler vertical slice
Requirement IDs: REQ-IDL-001, REQ-IDL-003, REQ-SEC-003, REQ-DOC-001, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0004, ADR-0005
Specification references: IDL-42-PREPROCESSING, IDL-42-LEXICAL
Target module: modules/corba-idl-parser
Allowed files: modules/corba-idl-parser/src/main/**, modules/corba-idl-parser/src/test/**, docs/architecture/idl-compiler-architecture.md
Forbidden files: full parser behavior, semantic model, code generation, filesystem writes outside explicit test fixtures
Expected behavior: Task type: implementation. Add include resolution and preprocessor support needed for deterministic IDL validation.
Tests to add/update: Unit/spec tests for include paths, cycle diagnostics, macro handling selected for the first compatibility profile, and stable source mapping.
Documentation to update: IDL compiler architecture and package docs.
Commands to run: ./gradlew :modules:corba-idl-parser:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Preprocessed token streams preserve source locations and reject unsafe include traversal.
Rollback notes: Revert preprocessor implementation, tests, and docs together.
