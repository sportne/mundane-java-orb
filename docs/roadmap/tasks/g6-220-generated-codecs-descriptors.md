# G6-220 Generated Codecs and Descriptors

Task ID: G6-220-GENERATED-CODECS-DESCRIPTORS
Status: draft
Gate: G6 generated-code vertical slice
Requirement IDs: REQ-IDLJ-004, REQ-NATIVE-002, REQ-NFR-001, REQ-DOC-005
ADR IDs: ADR-0005, ADR-0010
Specification references: CORBA-IOP-CDR, CORBA-IF-TYPECODE, I2JAV-13-PORTABILITY
Target module: modules/corba-codegen, modules/corba-cdr, modules/corba-typecode
Allowed files: modules/corba-codegen/src/**, modules/corba-cdr/src/**, modules/corba-typecode/src/**, docs/architecture/dynamic-corba-design.md
Forbidden files: ORB invocation runtime, IIOP transport, peer interop execution
Expected behavior: Task type: implementation. Generate static operation/type descriptors and CDR codec stubs for the Hello slice without requiring runtime reflection.
Tests to add/update: Generated-code compilation tests and descriptor shape unit tests.
Documentation to update: Native-image and dynamic CORBA design notes.
Commands to run: ./gradlew :modules:corba-codegen:test :modules:corba-cdr:test :modules:corba-typecode:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Generated descriptors are deterministic and suitable for later native-image validation.
Rollback notes: Revert descriptor/codegen changes, tests, and docs together.

