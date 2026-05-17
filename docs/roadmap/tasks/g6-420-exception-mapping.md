# G6-420 Exception Mapping

Task ID: G6-420-EXCEPTION-MAPPING
Status: complete
Gate: G6 local invocation vertical slice
Requirement IDs: REQ-ORB-001, REQ-IDLJ-002, REQ-CDR-001, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0003, ADR-0005
Specification references: CORBA-IF-ORB, I2JAV-13-EXCEPTIONS, CORBA-IOP-CDR
Target module: modules/corba-orb-core, modules/corba-omg-api, modules/corba-codegen
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-420-exception-mapping.md, docs/roadmap/tasks/g6-510-giop-messages.md, docs/architecture/runtime-architecture.md, docs/conformance/**, modules/corba-omg-api/build.gradle, modules/corba-omg-api/README.md, modules/corba-omg-api/src/**, modules/corba-orb-core/build.gradle, modules/corba-orb-core/README.md, modules/corba-orb-core/src/**, modules/corba-modern-api/README.md, modules/corba-modern-api/src/**, modules/corba-codegen/README.md, modules/corba-codegen/src/**
Forbidden files: IIOP transport, peer interop, optional services
Expected behavior: Task type: implementation. Define deterministic system and user exception mapping for generated local invocation and later wire replies.
Tests to add/update: Unit and generated-code tests for user exceptions, system exceptions, and diagnostic preservation.
Documentation to update: Runtime architecture and conformance rows.
Commands to run: ./gradlew :modules:corba-orb-core:test :modules:corba-codegen:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Local invocation handles successful and exceptional generated calls consistently.
Rollback notes: Revert exception mapping implementation, tests, and docs together.
