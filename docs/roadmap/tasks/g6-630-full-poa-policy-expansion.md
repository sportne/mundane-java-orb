# G6-630 Full POA Policy Expansion

Task ID: G6-630-FULL-POA-POLICY-EXPANSION
Gate: G6 server runtime expansion
Requirement IDs: REQ-POA-001, REQ-POA-002, REQ-ORB-001, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0003, ADR-0005
Specification references: CORBA-IF-POA, CORBA-IF-ORB
Target module: modules/corba-poa
Allowed files: modules/corba-poa/src/**, modules/corba-orb-core/src/**, docs/architecture/poa-design.md, docs/conformance/corba-3.4-matrix.md
Forbidden files: optional services, peer artifact downloads, unrelated ORB features
Expected behavior: Task type: implementation. Expand from POA-lite to the approved POA policy matrix, including managers, servant managers, locators, and adapter activators.
Tests to add/update: Unit, integration, negative, and policy-combination tests.
Documentation to update: POA design and conformance matrix status.
Commands to run: ./gradlew :modules:corba-poa:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Approved POA policies are implemented or explicitly deferred with documented reasons.
Rollback notes: Revert full POA expansion, tests, and docs together.

