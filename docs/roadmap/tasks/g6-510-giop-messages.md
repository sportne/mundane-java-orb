# G6-510 GIOP Messages

Task ID: G6-510-GIOP-MESSAGES
Status: draft
Gate: G6 wire invocation vertical slice
Requirement IDs: REQ-GIOP-001, REQ-SEC-001, REQ-SEC-002, REQ-SEC-003, REQ-DOC-004
ADR IDs: ADR-0002, ADR-0004, ADR-0005, ADR-0010
Specification references: CORBA-IOP-GIOP, CORBA-IOP-SERVICE-CONTEXT
Target module: modules/corba-giop
Allowed files: modules/corba-giop/src/main/**, modules/corba-giop/src/test/**, modules/corba-cdr/src/main/**, docs/architecture/cdr-giop-iiop.md
Forbidden files: TCP transport, ORB listener startup, peer interop execution
Expected behavior: Task type: implementation. Implement bounded GIOP message models and read/write support for request, reply, locate, close, error, cancel, and fragments.
Tests to add/update: Unit, negative, golden-wire, and security tests for message headers, service contexts, and fragmentation bounds.
Documentation to update: GIOP design notes and conformance rows.
Commands to run: ./gradlew :modules:corba-giop:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: GIOP messages can round-trip from bytes without opening sockets.
Rollback notes: Revert GIOP implementation, tests, and docs together.

