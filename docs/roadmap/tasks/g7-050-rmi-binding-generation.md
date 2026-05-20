# G7-050 RMI Binding Generation

Task ID: G7-050-RMI-BINDING-GENERATION
Status: complete
Gate: G7 RMI-IIOP and Java-to-IDL
Requirement IDs: REQ-RMI-001, REQ-IDLJ-002, REQ-IDLJ-004, REQ-NATIVE-002, REQ-DOC-005
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010, ADR-0013
Specification references: JAV2I-14, JAV2I-14-RMI-IDL, I2JAV-13-PORTABILITY
Target module: modules/corba-rmi-iiop, modules/corba-codegen, modules/corba-idl-java-mapping
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g7-050-rmi-binding-generation.md, docs/roadmap/tasks/g7-060-rmi-value-exception-marshaling.md, docs/architecture/rmi-iiop-java-to-idl.md, docs/architecture/idl-compiler-architecture.md, docs/conformance/idl-to-java-matrix.md, modules/corba-rmi-iiop/README.md, modules/corba-rmi-iiop/src/main/**, modules/corba-rmi-iiop/src/test/**, modules/corba-codegen/README.md, modules/corba-codegen/src/main/**, modules/corba-codegen/src/test/**, modules/corba-idl-java-mapping/README.md, modules/corba-idl-java-mapping/src/main/**, modules/corba-idl-java-mapping/src/test/**
Forbidden files: ORB network invocation, IIOP wire behavior, functional value marshaling, Java serialization marshaling, dynamic proxies, runtime bytecode generation, runtime classpath scanning, peer interop execution
Expected behavior: Task type: implementation. Generate deterministic Java adapter surfaces, helpers, holders, stubs, ties, skeleton placeholders, descriptors, and compile-safe bindings approved by ADR-0013.
Tests to add/update: Mapping tests, golden-source tests, generated-source compilation tests, deterministic ordering tests, Native Image forbidden-mechanism audits, and negative tests for unsupported generated surfaces.
Documentation to update: RMI-IIOP README/package docs, codegen and mapping docs, architecture notes, IDL-to-Java conformance rows, roadmap index, this task, and G7-060 status.
Commands to run: ./gradlew :modules:corba-rmi-iiop:test :modules:corba-codegen:test :modules:corba-idl-java-mapping:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Generated binding sources are deterministic and compile-safe, generated metadata is explicit, and no runtime wire invocation or Java serialization marshaling is added.
Rollback notes: Revert binding generation, tests, docs, and roadmap status updates together.
