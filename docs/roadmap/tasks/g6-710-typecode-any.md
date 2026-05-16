# G6-710 TypeCode and Any

Task ID: G6-710-TYPECODE-ANY
Status: draft
Gate: G6 dynamic and metadata vertical slice
Requirement IDs: REQ-DYN-001, REQ-CDR-001, REQ-NATIVE-002, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0005, ADR-0010
Specification references: CORBA-IF-TYPECODE, CORBA-IF-DYNANY, CORBA-IOP-CDR
Target module: modules/corba-typecode, modules/corba-any
Allowed files: modules/corba-typecode/src/**, modules/corba-any/src/**, modules/corba-cdr/src/**, docs/architecture/dynamic-corba-design.md
Forbidden files: DII/DSI behavior, reflection-based marshaling, ORB transport
Expected behavior: Task type: implementation. Implement generated-descriptor-backed TypeCode and Any support for the current IDL/CDR subset.
Tests to add/update: Unit and CDR round-trip tests for primitive, struct, enum, sequence, and exception Any values.
Documentation to update: Dynamic CORBA design, package docs, and conformance rows.
Commands to run: ./gradlew :modules:corba-typecode:test :modules:corba-any:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Any/TypeCode behavior uses static descriptors and remains native-image friendly.
Rollback notes: Revert TypeCode/Any implementation, tests, and docs together.

