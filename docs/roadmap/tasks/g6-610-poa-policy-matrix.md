# G6-610 POA Policy Matrix

Task ID: G6-610-POA-POLICY-MATRIX
Status: ready-for-implementation
Gate: G6 server runtime design
Requirement IDs: REQ-POA-001, REQ-POA-002, REQ-NFR-007
ADR IDs: ADR-0002, ADR-0004, ADR-0005
Specification references: CORBA-IF-POA, CORBA-IF-ORB
Target module: docs/architecture and docs/conformance
Allowed files: docs/architecture/poa-design.md, docs/conformance/corba-3.4-matrix.md, docs/roadmap/tasks/g6-610-poa-policy-matrix.md
Forbidden files: modules/**/src/main/**, runtime behavior, generated code
Expected behavior: Task type: design-only. Complete the POA policy-combination matrix before implementing POA behavior.
Tests to add/update: No product tests; define future POA test IDs and matrix coverage.
Documentation to update: POA design and conformance matrix.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: POA-lite and full POA task boundaries are explicit and reviewable.
Rollback notes: Revert POA design/matrix documentation together.
