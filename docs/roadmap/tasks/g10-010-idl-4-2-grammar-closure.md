# G10-010 IDL 4.2 Grammar Closure

Task ID: G10-010-IDL-4.2-GRAMMAR-CLOSURE
Status: ready-for-implementation
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-IDL-001, REQ-IDL-003, REQ-SEC-003, REQ-INTEROP-009
ADR IDs: ADR-0001, ADR-0005, ADR-0007, ADR-0008
Specification references: IDL-42-GRAMMAR, IDL-42-LEXICAL, IDL-42-PREPROCESSING, IDL-42-SCOPING
Target module: modules/corba-idl-ast, modules/corba-idl-parser, modules/corba-idl-semantics, modules/corba-idlj-cli
Allowed files: modules/corba-idl-ast/src/**, modules/corba-idl-parser/src/**, modules/corba-idl-semantics/src/**, modules/corba-idlj-cli/src/**, docs/architecture/idl-compiler-architecture.md, docs/conformance/idl-4.2-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-010-idl-4-2-grammar-closure.md, docs/roadmap/tasks/g10-020-idl-to-java-legacy-mapping-closure.md, README.md
Forbidden files: generated artifacts, peer artifacts, interop live reports, optional service implementation, runtime ORB/POA/IIOP behavior
Expected behavior: Task type: implementation. Extend the IDL front end for non-optional pre-1.0 peer fixtures: unions, sequences, arrays, typedefs and aliases, interface forward declarations, interface inheritance, recursive declarations, bounded preprocessor behavior needed by peer IDL corpora, deterministic diagnostics, and bounded hostile-input handling.
Tests to add/update: Unit tests for AST values, lexer/preprocessor/parser/semantic success and failure paths, CLI validation fixtures, shared IDL corpus parsing, malformed inputs, and regression coverage for no parser recovery hangs.
Documentation to update: IDL compiler architecture, IDL 4.2 conformance matrix, roadmap index, README ready-task status, this task, and G10-020 status when complete.
Commands to run: ./gradlew :modules:corba-idl-ast:test :modules:corba-idl-parser:test :modules:corba-idl-semantics:test :modules:corba-idlj-cli:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Peer fixture IDL needed by later G10 tasks parses and semantically validates with stable diagnostics; unsupported optional service IDL remains out of scope; Native Image and security source audits still pass through qualityGate.
Rollback notes: Revert IDL AST/parser/semantic/CLI changes, fixture updates, and docs together.
