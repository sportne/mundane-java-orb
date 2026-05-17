# G6-050 Testkit Golden Fixtures

Task ID: G6-050-TESTKIT-GOLDEN-FIXTURES
Status: complete
Gate: G6 foundation verification
Requirement IDs: REQ-NFR-007, REQ-DOC-006, REQ-SEC-003
ADR IDs: ADR-0004, ADR-0005, ADR-0007
Specification references: Verification infrastructure task; feature tasks cite exact OMG clauses.
Target module: modules/corba-testkit
Allowed files: modules/corba-testkit/src/main/**, modules/corba-testkit/src/test/**, docs/verification/**, interop/idl/**
Forbidden files: production runtime behavior, generated source committed as implementation, peer artifacts
Expected behavior: Task type: implementation. Provide reusable fixture loading, golden-file assertions, and test metadata conventions for IDL, generated-source, and wire tests.
Tests to add/update: Unit tests for fixture lookup, normalization, and assertion failure messages.
Documentation to update: Verification strategy and test taxonomy if new fixture conventions are introduced.
Commands to run: ./gradlew :modules:corba-testkit:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Later feature tests can reuse fixtures without duplicating file parsing or comparison logic.
Rollback notes: Revert testkit fixture helpers, tests, and verification docs together.
