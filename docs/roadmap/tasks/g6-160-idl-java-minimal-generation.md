# G6-160 Minimal IDL-to-Java Generation

Task ID: G6-160-IDL-JAVA-MINIMAL-GENERATION
Status: ready-for-implementation
Gate: G6 IDL compiler vertical slice
Requirement IDs: REQ-IDLJ-002, REQ-IDLJ-003, REQ-IDLJ-004, REQ-DOC-005
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010
Specification references: I2JAV-13-MODULES, I2JAV-13-BASIC, I2JAV-13-INTERFACES, I2JAV-13-PORTABILITY
Target module: modules/corba-idl-java-mapping, modules/corba-codegen
Allowed files: modules/corba-idl-java-mapping/src/main/**, modules/corba-idl-java-mapping/src/test/**, modules/corba-codegen/src/main/**, modules/corba-codegen/src/test/**, docs/conformance/idl-to-java-matrix.md
Forbidden files: ORB runtime, protocol transport, real peer interop, runtime bytecode generation
Expected behavior: Task type: implementation. Generate deterministic Java source for the minimal IDL subset in legacy and modern modes where approved for the slice.
Tests to add/update: Golden-source tests and generated-source compilation tests.
Documentation to update: Mapping package docs and IDL-to-Java conformance rows.
Commands to run: ./gradlew :modules:corba-idl-java-mapping:test :modules:corba-codegen:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Generated source is deterministic, documented, and does not require reflection in normal paths.
Rollback notes: Revert mapping/codegen implementation, tests, and conformance updates together.
