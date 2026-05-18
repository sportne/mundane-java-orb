# G6-720 DynamicAny, DII, and DSI

Task ID: G6-720-DYNAMICANY-DII-DSI
Status: ready-for-implementation
Gate: G6 dynamic and metadata vertical slice
Requirement IDs: REQ-DYN-001, REQ-ORB-001, REQ-NATIVE-002, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0005, ADR-0010
Specification references: CORBA-IF-DYNANY, CORBA-IF-DII, CORBA-IF-DSI
Target module: modules/corba-dynamic
Allowed files: modules/corba-dynamic/src/**, modules/corba-any/src/**, modules/corba-typecode/src/**, docs/architecture/dynamic-corba-design.md
Forbidden files: reflection-driven normal invocation, runtime bytecode generation, peer interop execution
Expected behavior: Task type: implementation. Implement DynamicAny plus descriptor-backed DII/DSI for the supported operation/type subset.
Tests to add/update: Unit and integration tests for dynamic value construction, invocation, skeleton dispatch, and negative type errors.
Documentation to update: Dynamic CORBA design and package docs.
Commands to run: ./gradlew :modules:corba-dynamic:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Dynamic behavior works over static descriptors and does not add broad reflection requirements.
Rollback notes: Revert dynamic CORBA implementation, tests, and docs together.
