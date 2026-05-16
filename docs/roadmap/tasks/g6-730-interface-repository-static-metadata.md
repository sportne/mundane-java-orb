# G6-730 Interface Repository Static Metadata

Task ID: G6-730-INTERFACE-REPOSITORY-STATIC-METADATA
Gate: G6 dynamic and metadata vertical slice
Requirement IDs: REQ-DYN-001, REQ-IDLJ-004, REQ-NATIVE-002, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0005, ADR-0010
Specification references: CORBA-IF-IR, CORBA-IF-TYPECODE
Target module: modules/corba-interface-repository
Allowed files: modules/corba-interface-repository/src/**, modules/corba-codegen/src/**, docs/architecture/dynamic-corba-design.md
Forbidden files: networked repository service unless explicitly approved, reflection-based classpath scanning
Expected behavior: Task type: implementation. Provide a static metadata bridge over generated descriptors before any networked Interface Repository behavior.
Tests to add/update: Unit and generated-code tests for descriptor lookup and repository ID linking.
Documentation to update: Dynamic CORBA and interface repository design notes.
Commands to run: ./gradlew :modules:corba-interface-repository:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Metadata lookup is deterministic and does not scan the runtime classpath.
Rollback notes: Revert static metadata bridge, tests, and docs together.

