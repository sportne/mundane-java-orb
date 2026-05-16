# G6-320 CDR Strings, Sequences, and Encapsulations

Task ID: G6-320-CDR-STRINGS-SEQUENCES-ENCAPSULATIONS
Gate: G6 CDR and IOR vertical slice
Requirement IDs: REQ-CDR-001, REQ-SEC-001, REQ-SEC-002, REQ-SEC-003, REQ-DOC-004
ADR IDs: ADR-0002, ADR-0004, ADR-0005, ADR-0010
Specification references: CORBA-IOP-CDR, CORBA-IF-TYPECODE
Target module: modules/corba-cdr
Allowed files: modules/corba-cdr/src/main/**, modules/corba-cdr/src/test/**, modules/corba-testkit/src/**, docs/architecture/cdr-giop-iiop.md
Forbidden files: GIOP/IIOP transport, ORB runtime, generated code behavior outside tests
Expected behavior: Task type: implementation. Add bounded string, sequence, array helper, and encapsulation handling for generated codecs and IOR parsing.
Tests to add/update: Unit, negative, golden-wire, and security tests for length limits and malformed data.
Documentation to update: CDR design and conformance notes.
Commands to run: ./gradlew :modules:corba-cdr:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Length-bearing values validate configured limits before allocation.
Rollback notes: Revert CDR collection/encapsulation implementation, tests, and docs together.

