# G6-930 Compatibility, Security, and Performance Closure

Task ID: G6-930-COMPATIBILITY-SECURITY-PERFORMANCE-CLOSURE
Status: complete
Gate: G6 release hardening
Requirement IDs: REQ-NFR-004, REQ-NFR-005, REQ-NFR-007, REQ-SEC-001, REQ-SEC-002, REQ-SEC-003, REQ-INTEROP-009
ADR IDs: ADR-0002, ADR-0003, ADR-0004, ADR-0006, ADR-0010, ADR-0011
Specification references: CORBA-IF, CORBA-IOP, IDL-42, I2JAV-13, NAM-13
Target module: whole repository
Allowed files: modules/**/src/test/**, docs/conformance/**, docs/verification/**, interop/**, README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-930-compatibility-security-performance-closure.md
Forbidden files: new feature behavior not tied to an accepted requirement, reference source copying, unreviewed generated artifacts
Expected behavior: Task type: verification-only. Close compatibility profiles, conformance statuses, security/fuzz coverage, performance baselines, and structured failure reporting before release.
Tests to add/update: Fuzz, security, interop, native-image, performance, and soak tests according to the verification strategy.
Documentation to update: Conformance matrices, verification strategy, interop matrix, Native Image matrix, G6 release hardening closure evidence, roadmap index, and README.
Commands to run: ./gradlew :modules:corba-cdr:test :modules:corba-giop:test :modules:corba-ior:test :modules:corba-idl-parser:test :modules:corba-iiop:test; ./gradlew :modules:corba-interop-testkit:test :modules:corba-native-image:test; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer validate-gates; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Conformance and verification records match implemented behavior and remaining gaps are explicitly deferred.
Rollback notes: Revert closure documentation and verification changes together.
