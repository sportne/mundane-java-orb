# G7-020 Java-to-IDL Model

Task ID: G7-020-JAVA-TO-IDL-MODEL
Status: complete
Gate: G7 RMI-IIOP and Java-to-IDL
Requirement IDs: REQ-RMI-001, REQ-IDLJ-002, REQ-IDLJ-004, REQ-DOC-001
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010, ADR-0013
Specification references: JAV2I-14, JAV2I-14-RMI-IDL
Target module: modules/corba-rmi-iiop
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g7-020-java-to-idl-model.md, docs/roadmap/tasks/g7-030-rmi-repository-id-hashes.md, docs/architecture/rmi-iiop-java-to-idl.md, docs/conformance/idl-to-java-matrix.md, modules/corba-rmi-iiop/README.md, modules/corba-rmi-iiop/src/main/**, modules/corba-rmi-iiop/src/test/**
Forbidden files: generated source emission, ORB invocation, IIOP wire behavior, CDR marshaling behavior, stubs, ties, skeletons, dynamic proxies, runtime reflection, classpath scanning, Java serialization marshaling, peer interop execution
Expected behavior: Task type: implementation. Add a deterministic Java-to-IDL mapping model for eligible modules, interfaces, operations, exceptions, values, arrays, sequences, names, and unsupported constructs.
Tests to add/update: Unit tests for model construction, name mapping, operation signatures, exception/value references, unsupported constructs, deterministic ordering, and diagnostic propagation from G7-010.
Documentation to update: Module README, package docs, architecture notes, IDL-to-Java conformance matrix, roadmap index, this task, and G7-030 status.
Commands to run: ./gradlew :modules:corba-rmi-iiop:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Eligible declarations map to explicit immutable Java-to-IDL model records, unsupported constructs produce stable diagnostics, and no generated files, ORB calls, wire behavior, or runtime discovery are added.
Rollback notes: Revert mapping model, tests, docs, and roadmap status updates together.
