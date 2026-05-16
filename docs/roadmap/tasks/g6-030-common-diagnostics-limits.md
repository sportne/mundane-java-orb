# G6-030 Common Diagnostics and Limits

Task ID: G6-030-COMMON-DIAGNOSTICS-LIMITS
Status: complete
Gate: G6 foundation implementation
Requirement IDs: REQ-IDL-003, REQ-SEC-001, REQ-SEC-002, REQ-NFR-004, REQ-DOC-001, REQ-DOC-003
ADR IDs: ADR-0004, ADR-0005, ADR-0010
Specification references: IDL-42-LEXICAL, IDL-42-GRAMMAR, CORBA-IOP-CDR, CORBA-IOP-GIOP
Target module: modules/corba-common
Allowed files: modules/corba-common/src/main/**, modules/corba-common/src/test/**, modules/corba-common/README.md, docs/architecture/**
Forbidden files: protocol encoders/decoders outside common, ORB runtime, IDL parser behavior outside diagnostics support
Expected behavior: Task type: implementation. Provide stable diagnostic-code, source-position, and bounded-limit value objects usable by parser and protocol slices.
Tests to add/update: Unit tests for immutability, equality, validation, and message formatting.
Documentation to update: Package docs and architecture notes for shared diagnostics and limits.
Commands to run: ./gradlew :modules:corba-common:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Common types are documented, warning-free, covered by unit tests, and do not introduce reflection or serialization dependencies.
Rollback notes: Revert common diagnostics and limit types with their tests/docs.

