# G7-030 RMI Repository ID Hashes

Task ID: G7-030-RMI-REPOSITORY-ID-HASHES
Status: blocked
Gate: G7 RMI-IIOP and Java-to-IDL
Requirement IDs: REQ-RMI-001, REQ-IDLJ-004, REQ-SEC-003, REQ-DOC-001
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010, ADR-0013
Specification references: JAV2I-14-RMI-IDL, CORBA-IF-IR
Target module: modules/corba-repository-id, modules/corba-rmi-iiop
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g7-030-rmi-repository-id-hashes.md, docs/roadmap/tasks/g7-040-generated-idl-fixtures.md, docs/architecture/rmi-iiop-java-to-idl.md, docs/conformance/corba-2.3-legacy-java-matrix.md, modules/corba-repository-id/README.md, modules/corba-repository-id/src/main/**, modules/corba-repository-id/src/test/**, modules/corba-rmi-iiop/README.md, modules/corba-rmi-iiop/src/main/**, modules/corba-rmi-iiop/src/test/**
Forbidden files: ORB invocation, IIOP wire behavior, generated source emission, stubs, ties, skeletons, value marshaling, Java serialization marshaling, dynamic runtime class loading, peer interop execution
Expected behavior: Task type: implementation. Implement deterministic RMI repository ID construction, serialVersionUID/hash handling, validation, and diagnostics for approved Java-to-IDL model inputs.
Tests to add/update: Unit and negative tests for RMI repository ID formats, deterministic hash inputs, explicit serialVersionUID values, missing/invalid hash inputs, malformed repository IDs, and hostile length/name inputs.
Documentation to update: Repository ID README/package docs, RMI-IIOP README/package docs, architecture notes, legacy conformance row, roadmap index, this task, and G7-040 status.
Commands to run: ./gradlew :modules:corba-repository-id:test :modules:corba-rmi-iiop:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: RMI repository IDs are constructed and validated deterministically from explicit metadata, invalid inputs fail with stable diagnostics, and Java serialization is not used for marshaling.
Rollback notes: Revert repository ID/hash implementation, tests, docs, and roadmap status updates together.
