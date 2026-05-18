# G6-920 Offline Release Validation

Task ID: G6-920-OFFLINE-RELEASE-VALIDATION
Status: ready-for-implementation
Gate: G6 release hardening
Requirement IDs: REQ-OFFLINE-001, REQ-OFFLINE-002, REQ-OFFLINE-003, REQ-OFFLINE-004, REQ-OFFLINE-005, REQ-OFFLINE-006, REQ-BUILD-010
ADR IDs: ADR-0009, ADR-0011, ADR-0012
Specification references: Build/release validation task; no direct OMG clause.
Target module: build and release infrastructure
Allowed files: build-logic/**, gradle/**, docs/build/**, docs/verification/offline-build-validation.md, modules/corba-bom/**
Forbidden files: CORBA runtime behavior, protocol behavior, IDL compiler behavior, generated code committed as source
Expected behavior: Task type: verification-only. Validate offline builds, dependency verification, publication dry runs, BOM alignment, and sample downstream consumption.
Tests to add/update: Offline-build tagged validation and sample consumer checks.
Documentation to update: Offline build validation and build architecture docs.
Commands to run: ./gradlew --offline -Pcorba.offlineRepo=/path/to/local-maven-repo clean qualityGate; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Release validation works without network access when supplied with approved local inputs.
Rollback notes: Revert offline/release validation changes and docs together.
