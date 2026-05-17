# G6-110 IDL Diagnostics and Lexer

Task ID: G6-110-IDL-DIAGNOSTICS-LEXER
Status: ready-for-implementation
Gate: G6 IDL compiler vertical slice
Requirement IDs: REQ-IDL-001, REQ-IDL-003, REQ-SEC-003, REQ-DOC-001, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0004, ADR-0005, ADR-0007
Specification references: IDL-42-LEXICAL
Target module: modules/corba-idl-parser
Allowed files: modules/corba-idl-parser/src/main/**, modules/corba-idl-parser/src/test/**, docs/conformance/idl-4.2-matrix.md
Forbidden files: parser grammar beyond tokenization, semantic model, code generation, runtime behavior
Expected behavior: Task type: implementation. Implement IDL source locations, lexer tokens, lexical diagnostics, and bounded lexical scanning.
Tests to add/update: Unit/spec/negative tests for identifiers, keywords, literals, comments, whitespace, and malformed lexical input.
Documentation to update: Package docs and IDL lexical conformance row.
Commands to run: ./gradlew :modules:corba-idl-parser:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Lexer emits stable tokens and diagnostics without parsing declarations.
Rollback notes: Revert lexer implementation, tests, and conformance updates together.
