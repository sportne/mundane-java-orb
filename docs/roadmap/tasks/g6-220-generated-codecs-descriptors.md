# G6-220 Generated Codecs and Descriptors

Task ID: G6-220-GENERATED-CODECS-DESCRIPTORS
Status: complete
Gate: G6 generated-code vertical slice
Requirement IDs: REQ-IDLJ-004, REQ-NATIVE-002, REQ-NFR-001, REQ-DOC-005
ADR IDs: ADR-0005, ADR-0010
Specification references: CORBA-IOP-CDR, CORBA-IF-TYPECODE, I2JAV-13-PORTABILITY
Target module: modules/corba-codegen, modules/corba-cdr, modules/corba-typecode
Allowed files: modules/corba-codegen/build.gradle, modules/corba-codegen/src/**, modules/corba-codegen/README.md, modules/corba-cdr/src/**, modules/corba-typecode/build.gradle, modules/corba-typecode/src/**, modules/corba-typecode/README.md, docs/architecture/dynamic-corba-design.md, docs/architecture/idl-compiler-architecture.md, docs/architecture/native-image-design.md, docs/conformance/corba-3.4-matrix.md, docs/conformance/idl-to-java-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-220-generated-codecs-descriptors.md, docs/roadmap/tasks/g6-320-cdr-strings-sequences-encapsulations.md, README.md
Forbidden files: ORB invocation runtime, IIOP transport, peer interop execution
Expected behavior: Task type: implementation. Generate static operation/type descriptors and compile-only CDR codec surfaces for the Hello slice without requiring runtime reflection.
Tests to add/update: Generated-code compilation tests, descriptor shape unit tests, and a narrow Native Image smoke check.
Documentation to update: Native-image, IDL compiler, dynamic CORBA, module README, roadmap, and conformance notes.
Commands to run: ./gradlew :modules:corba-codegen:test :modules:corba-cdr:test :modules:corba-typecode:test; ./gradlew :modules:corba-typecode:nativeTypecodeDescriptorSmoke; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Generated descriptors are deterministic, compile-only codec stubs fail predictably, and the descriptor surface is validated through a narrow Native Image smoke binary.
Rollback notes: Revert descriptor/codegen changes, tests, and docs together.
