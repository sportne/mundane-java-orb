# G9-010 Exhaustive Test Hardening

Task ID: G9-010-EXHAUSTIVE-TEST-HARDENING
Status: complete
Gate: G9 verification hardening
Requirement IDs: REQ-DOC-006, REQ-NFR-007, REQ-SEC-003, REQ-NATIVE-002
ADR IDs: ADR-0001, ADR-0004, ADR-0005, ADR-0007, ADR-0008, ADR-0010
Specification references: Verification-only task; feature tests cite the existing CORBA, IDL, Naming, Java mapping, RMI-IIOP, and operational references they exercise.
Target module: implemented library modules under modules/**
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g9-010-exhaustive-test-hardening.md, docs/verification/verification-index.md, docs/verification/g9-exhaustive-test-hardening.md, modules/**/src/test/**, modules/corba-idl-parser/src/main/java/io/github/mundanej/mjo/idl/parser/IdlParser.java for the verified parser recovery hang only
Forbidden files: production runtime behavior, production public APIs, generated production artifacts, protocol or wire-format changes, Gradle dependency/plugin changes, optional CORBA service implementation, live peer execution requirements, subagent edits outside assigned test files
Expected behavior: Task type: verification-only. Expand unit and limited local JUnit integration coverage for implemented library behavior using bounded disjoint test shards while preserving existing public contracts.
Tests to add/update: Focused unit tests for production classes and grouped value/codec families; limited local integration tests for cross-module flows that run through the existing JUnit test source sets.
Documentation to update: Roadmap index, README ready-task state, verification index, and G9 test-hardening evidence.
Commands to run: ./gradlew test; ./gradlew qualityGate; ./gradlew validateDesignControlPack; git diff --check
Acceptance criteria: Implemented modules receive substantially broader deterministic test coverage; all required local tests pass; no excluded-tag or external-environment tests are required; only this G9 task is active while work is in progress; no product behavior changes are introduced except the narrowly reviewed parser recovery fix proven by failing tests.
Rollback notes: Revert G9 test additions and G9 verification/roadmap documentation together.
