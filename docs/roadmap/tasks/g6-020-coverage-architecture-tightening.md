# G6-020 Coverage and Architecture Tightening

Task ID: G6-020-COVERAGE-ARCHITECTURE-TIGHTENING
Status: complete
Gate: G6 validation hardening
Requirement IDs: REQ-BUILD-007, REQ-BUILD-008, REQ-NFR-003, REQ-NFR-007
ADR IDs: ADR-0004, ADR-0008
Specification references: Architecture and coverage policy task; no direct OMG clause.
Target module: build-logic and modules/corba-architecture-tests
Allowed files: build-logic/src/main/groovy/corba.coverage-conventions.gradle, build-logic/src/main/groovy/corba.docs-validation-conventions.gradle, modules/corba-architecture-tests/src/test/java/**, docs/verification/coverage-policy.md, docs/verification/g5-validation-gate-readiness.md, docs/architecture/module-boundaries.md, docs/roadmap/**, README.md
Forbidden files: runtime behavior, protocol behavior, IDL behavior, compiler behavior, generated code
Expected behavior: Task type: verification-only. Plan and then incrementally remove scaffold tolerances as real packages appear, without blocking empty modules prematurely.
Tests to add/update: ArchUnit tests and coverage verification checks for modules that now contain production classes.
Documentation to update: Coverage policy and module boundary enforcement notes.
Commands to run: ./gradlew :modules:corba-architecture-tests:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Tightening is tied to concrete implementation packages and does not create false failures for untouched modules.
Rollback notes: Revert coverage/architecture tightening as one validation changeset.
