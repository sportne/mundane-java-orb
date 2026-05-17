# G6-320 CDR Strings, Sequences, and Encapsulations

Task ID: G6-320-CDR-STRINGS-SEQUENCES-ENCAPSULATIONS
Status: complete
Gate: G6 CDR and IOR vertical slice
Requirement IDs: REQ-CDR-001, REQ-SEC-001, REQ-SEC-002, REQ-SEC-003, REQ-DOC-004
ADR IDs: ADR-0002, ADR-0004, ADR-0005, ADR-0010
Specification references: CORBA-IOP-CDR, CORBA-IF-TYPECODE
Target module: modules/corba-cdr
Allowed files: modules/corba-cdr/build.gradle, modules/corba-cdr/README.md, modules/corba-cdr/src/main/**, modules/corba-cdr/src/nativeSmoke/**, modules/corba-cdr/src/test/**, modules/corba-testkit/src/**, docs/architecture/cdr-giop-iiop.md, docs/conformance/corba-3.4-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-320-cdr-strings-sequences-encapsulations.md, docs/roadmap/tasks/g6-330-ior-profiles-object-urls.md, README.md
Forbidden files: GIOP/IIOP transport, ORB runtime, generated code behavior outside tests
Expected behavior: Task type: implementation. Add bounded narrow string, sequence-length, fixed-array helper, octet-sequence, and encapsulation handling for generated codecs and IOR parsing.
Tests to add/update: Unit, negative, golden-wire, and security tests for length limits and malformed data.
Documentation to update: CDR design and conformance notes.
Commands to run: ./gradlew :modules:corba-cdr:test; ./gradlew :modules:corba-cdr:nativeCdrSmoke; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Length-bearing values validate configured limits before allocation.
Rollback notes: Revert CDR collection/encapsulation implementation, tests, and docs together.
