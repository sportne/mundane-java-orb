# G12-020 IDL Grammar, Valuetype, And Pragma Hardening

Task ID: G12-020-IDL-GRAMMAR-VALUETYPE-PRAGMAS
Status: ready-for-implementation
Gate: G12 post-1.0 compiler and interop hardening
Requirement IDs: REQ-IDL-001, REQ-IDL-002, REQ-IDL-003, REQ-IDLJ-001, REQ-SEC-003
ADR IDs: ADR-0001, ADR-0005, ADR-0007, ADR-0008
Specification references: IDL-42-GRAMMAR, IDL-42-SCOPING, IDL-42-PREPROCESSING
Target module: modules/corba-idl-ast, modules/corba-idl-parser, modules/corba-idl-semantics, modules/corba-idlj-cli
Allowed files: modules/corba-idl-ast/src/**, modules/corba-idl-parser/src/**, modules/corba-idl-semantics/src/**, modules/corba-idlj-cli/src/**, modules/corba-testkit/src/**, docs/architecture/idl-compiler-architecture.md, docs/conformance/idl-4.2-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g12-020-idl-grammar-valuetype-pragmas.md, docs/roadmap/tasks/g12-030-idl-semantic-type-system-closure.md, README.md
Forbidden files: generated artifacts, peer artifacts, interop live reports, optional service implementation, runtime ORB/POA/IIOP behavior, CDR/GIOP/IIOP runtime behavior
Expected behavior: Task type: implementation. Extend the parser and AST for post-1.0 IDL constructs needed before richer compiler and interop fixtures: valuetypes, value boxes, abstract and local interfaces, native declarations, pragmas including repository ID affecting forms, operation context clauses, additional declarator forms, and deterministic recovery diagnostics for malformed compound declarations.
Tests to add/update: Parser, AST, semantic acceptance and rejection tests; validate-CLI fixture tests; malformed fixture tests; hostile nesting-depth tests; conformance fixture entries for each newly accepted construct.
Documentation to update: IDL compiler architecture, IDL 4.2 conformance matrix, roadmap index, README ready-task status, this task, and G12-030 status when complete.
Commands to run: ./gradlew :modules:corba-idl-ast:test :modules:corba-idl-parser:test :modules:corba-idl-semantics:test :modules:corba-idlj-cli:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: The validate-only CLI accepts the approved richer IDL grammar corpus with stable semantic models and rejects unsupported or malformed variants with stable diagnostics; G12-030 is promoted to ready-for-implementation.
Rollback notes: Revert IDL AST/parser/semantic/CLI fixture changes and documentation updates together.
