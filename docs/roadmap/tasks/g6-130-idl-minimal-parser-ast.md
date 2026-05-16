# G6-130 Minimal IDL Parser and AST

Task ID: G6-130-IDL-MINIMAL-PARSER-AST
Status: draft
Gate: G6 IDL compiler vertical slice
Requirement IDs: REQ-IDL-001, REQ-IDL-002, REQ-IDL-003, REQ-DOC-001, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0004, ADR-0005
Specification references: IDL-42-GRAMMAR, IDL-42-SCOPING
Target module: modules/corba-idl-parser, modules/corba-idl-ast
Allowed files: modules/corba-idl-parser/src/main/**, modules/corba-idl-parser/src/test/**, modules/corba-idl-ast/src/main/**, modules/corba-idl-ast/src/test/**, docs/conformance/idl-4.2-matrix.md
Forbidden files: semantic type checking, code generation, runtime behavior, protocol behavior
Expected behavior: Task type: implementation. Parse a minimal vertical subset covering modules, interfaces, operations, attributes, structs, enums, exceptions, and constants into immutable AST nodes.
Tests to add/update: Spec/golden-source/negative parser tests for accepted and rejected IDL snippets.
Documentation to update: AST package docs and IDL conformance rows.
Commands to run: ./gradlew :modules:corba-idl-parser:test :modules:corba-idl-ast:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Minimal AST is deterministic, documented, and independent of code generation.
Rollback notes: Revert parser/AST implementation, tests, and docs together.

