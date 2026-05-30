# G12-010 IDL Preprocessor Hardening

Task ID: G12-010-IDL-PREPROCESSOR-HARDENING
Status: complete
Gate: G12 post-1.0 compiler and interop hardening
Requirement IDs: REQ-IDL-001, REQ-IDL-003, REQ-IDLJ-001, REQ-SEC-003, REQ-NATIVE-005
ADR IDs: ADR-0001, ADR-0005, ADR-0007, ADR-0008, ADR-0010
Specification references: IDL-42-LEXICAL, IDL-42-PREPROCESSING
Target module: modules/corba-idl-parser, modules/corba-idlj-cli
Allowed files: modules/corba-idl-parser/src/**, modules/corba-idlj-cli/src/**, modules/corba-testkit/src/**, docs/architecture/idl-compiler-architecture.md, docs/conformance/idl-4.2-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g12-010-idl-preprocessor-hardening.md, docs/roadmap/tasks/g12-020-idl-grammar-valuetype-pragmas.md, README.md
Forbidden files: generated artifacts, peer artifacts, interop live reports, optional service implementation, runtime ORB/POA/IIOP behavior, IDL-to-Java code generation behavior outside diagnostics needed by validate-only CLI output
Expected behavior: Task type: implementation. Harden the IDL preprocessor for the next compiler lane: nested includes with deterministic source maps, include guards, macro expansion limits, object-like and function-like macro diagnostics, conditional-expression diagnostics, line markers, bounded recursion, and explicit unsupported diagnostics for token-pasting, stringification, and variadic macro forms if they remain out of scope.
Tests to add/update: Preprocessor unit tests, validate-CLI fixture tests, hostile-input tests for recursive includes and macro expansion bounds, source-location regression tests, and Native Image smoke coverage for representative validate-only preprocessing fixtures if the existing native lane exposes the CLI surface.
Documentation to update: IDL compiler architecture, IDL 4.2 conformance matrix, roadmap index, README ready-task status, this task, and G12-020 status when complete.
Commands to run: ./gradlew :modules:corba-idl-parser:test :modules:corba-idlj-cli:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: The validate-only CLI reports stable source locations and diagnostic codes for nested include and macro cases; hostile preprocessing inputs fail without unbounded recursion or allocation; unsupported preprocessor constructs are explicitly diagnosed; G12-020 is promoted to ready-for-implementation.
Completion evidence: Completed on 2026-05-30. The IDL preprocessor now recognizes `#line` and C-style `# <line> "source"` line markers for deterministic following-token source remapping, reports malformed active line markers with `IDL-0215`, keeps identity source spans unchanged when no marker exists, preserves include-guarded nested include behavior, and reports variadic macro spellings as explicit unsupported macro operators. Focused parser and CLI tests passed with line-marker, include-guard, variadic macro, and CLI diagnostic coverage.
Rollback notes: Revert IDL parser/CLI/testkit fixture changes and documentation updates together.
