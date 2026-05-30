# G12-040 IDL-To-Java Mapping Hardening

Task ID: G12-040-IDL-TO-JAVA-MAPPING-HARDENING
Status: ready-for-implementation
Gate: G12 post-1.0 compiler and interop hardening
Requirement IDs: REQ-IDL-001, REQ-IDL-002, REQ-IDLJ-002, REQ-IDLJ-003, REQ-IDLJ-004, REQ-DOC-005, REQ-NATIVE-002, REQ-SEC-004
ADR IDs: ADR-0001, ADR-0005, ADR-0007, ADR-0008, ADR-0010
Specification references: I2JAV-13-MODULES, I2JAV-13-BASIC, I2JAV-13-HELPERS, I2JAV-13-TYPES, I2JAV-13-INTERFACES, I2JAV-13-SERVER, I2JAV-13-PORTABILITY, IDL-42-GRAMMAR
Target module: modules/corba-idl-java-mapping, modules/corba-codegen, modules/corba-typecode
Allowed files: modules/corba-idl-java-mapping/src/**, modules/corba-codegen/src/**, modules/corba-typecode/src/**, modules/corba-testkit/src/**, docs/architecture/idl-compiler-architecture.md, docs/conformance/idl-to-java-matrix.md, docs/conformance/idl-4.2-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g12-040-idl-to-java-mapping-hardening.md, docs/roadmap/tasks/g12-050-wide-idl-feature-interop-corpus.md, README.md
Forbidden files: peer artifacts, interop live reports, optional service implementation, runtime ORB/POA/IIOP behavior, Java serialization marshaling, reflection-driven generation, runtime bytecode generation
Expected behavior: Task type: implementation. Extend legacy and modern generated source surfaces for the approved richer IDL subset while preserving build-time generation and Native Image posture: helpers, holders, descriptors, TypeCode metadata, stubs, skeleton placeholders, codecs where already supported, deterministic source headers, and compile-safe output for the broad IDL feature corpus.
Tests to add/update: Mapping tests, Java source generation golden tests, generated-source compilation tests, descriptor/TypeCode tests, native-boundary tests for generated metadata, and negative tests for unsupported mapping features.
Documentation to update: IDL compiler architecture, IDL-to-Java conformance matrix, IDL 4.2 conformance matrix, roadmap index, README ready-task status, this task, and G12-050 status when complete.
Commands to run: ./gradlew :modules:corba-idl-java-mapping:test :modules:corba-codegen:test :modules:corba-typecode:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: The approved richer IDL corpus generates deterministic legacy and modern Java sources that compile without reflection, runtime bytecode generation, or Java serialization marshaling; unsupported mapping constructs fail with stable diagnostics; G12-050 is promoted to ready-for-implementation.
Rollback notes: Revert mapping/codegen/typecode fixture changes and documentation updates together.
