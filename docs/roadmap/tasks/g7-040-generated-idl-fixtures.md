# G7-040 Generated IDL Fixtures

Task ID: G7-040-GENERATED-IDL-FIXTURES
Status: blocked
Gate: G7 RMI-IIOP and Java-to-IDL
Requirement IDs: REQ-RMI-001, REQ-IDL-001, REQ-IDLJ-002, REQ-DOC-005
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010, ADR-0013
Specification references: JAV2I-14, JAV2I-14-RMI-IDL, IDL-42-GRAMMAR
Target module: modules/corba-rmi-iiop, modules/corba-idl-parser, modules/corba-idl-semantics
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g7-040-generated-idl-fixtures.md, docs/roadmap/tasks/g7-050-rmi-binding-generation.md, docs/architecture/rmi-iiop-java-to-idl.md, docs/conformance/idl-4.2-matrix.md, docs/conformance/idl-to-java-matrix.md, modules/corba-rmi-iiop/README.md, modules/corba-rmi-iiop/src/main/**, modules/corba-rmi-iiop/src/test/**, modules/corba-idl-parser/src/test/**, modules/corba-idl-semantics/src/test/**
Forbidden files: Java binding generation, stubs, ties, skeletons, ORB invocation, IIOP wire behavior, CDR marshaling behavior, peer interop execution
Expected behavior: Task type: implementation. Emit deterministic generated IDL text fixtures from approved Java-to-IDL models and validate those fixtures through the existing IDL parser and semantic analyzer.
Tests to add/update: Golden IDL tests, generated fixture parsing tests, semantic validation tests, deterministic ordering tests, and negative tests for unsupported Java-to-IDL model inputs.
Documentation to update: RMI-IIOP architecture notes, module README/package docs, IDL and IDL-to-Java conformance rows, roadmap index, this task, and G7-050 status.
Commands to run: ./gradlew :modules:corba-rmi-iiop:test :modules:corba-idl-parser:test :modules:corba-idl-semantics:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Approved Java-to-IDL models produce stable IDL golden fixtures that parse and semantically validate without adding Java binding generation or runtime invocation behavior.
Rollback notes: Revert IDL fixture generation, tests, docs, and roadmap status updates together.
