# G6-040 Repository ID Foundation

Task ID: G6-040-REPOSITORY-ID-FOUNDATION
Status: complete
Gate: G6 foundation implementation
Requirement IDs: REQ-IOR-001, REQ-IDL-002, REQ-NFR-007, REQ-DOC-001, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0005, ADR-0012
Specification references: CORBA-IF-IR section 14.7, CORBA-IOP-IOR, CORBA-IF-OBJECT-REF, IDL-42-SCOPING
Target module: modules/corba-repository-id
Allowed files: modules/corba-repository-id/src/main/**, modules/corba-repository-id/src/test/**, modules/corba-repository-id/README.md, docs/conformance/**, docs/roadmap/**
Forbidden files: IOR binary parsing, ORB runtime behavior, IDL compiler behavior beyond repository ID value rules
Expected behavior: Task type: implementation. Implement repository ID parsing, validation, normalization, and construction for later IDL, IOR, and runtime slices.
Tests to add/update: Unit and negative tests for valid IDs, malformed IDs, version normalization, and deterministic formatting.
Documentation to update: Package docs and affected conformance notes.
Commands to run: ./gradlew :modules:corba-repository-id:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Repository ID behavior is deterministic, documented, and usable independently.
Rollback notes: Revert repository ID implementation, tests, and docs together.
