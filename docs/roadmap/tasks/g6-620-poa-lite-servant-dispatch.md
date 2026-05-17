# G6-620 POA-Lite Servant Dispatch

Task ID: G6-620-POA-LITE-SERVANT-DISPATCH
Status: ready-for-implementation
Gate: G6 server runtime vertical slice
Requirement IDs: REQ-POA-001, REQ-ORB-001, REQ-IDLJ-004, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010
Specification references: CORBA-IF-POA, CORBA-IF-ORB
Target module: modules/corba-poa, modules/corba-orb-core
Allowed files: modules/corba-poa/src/**, modules/corba-orb-core/src/**, docs/architecture/poa-design.md
Forbidden files: full POA policy matrix behavior, optional services, peer interop
Expected behavior: Task type: implementation. Implement the approved POA-lite subset needed for generated skeleton dispatch in local and loopback Hello slices.
Tests to add/update: Unit and integration tests for servant activation, object IDs, dispatch, and shutdown.
Documentation to update: POA package docs and POA-lite matrix rows.
Commands to run: ./gradlew :modules:corba-poa:test :modules:corba-orb-core:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Generated skeletons can dispatch through POA-lite with documented policy limitations.
Rollback notes: Revert POA-lite implementation, tests, and docs together.
