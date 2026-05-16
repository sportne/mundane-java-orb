# G6-D10 RMI-IIOP and Java-to-IDL

Task ID: G6-D10-RMI-IIOP-JAVA-TO-IDL
Gate: Deferred compatibility gate
Requirement IDs: REQ-RMI-001
ADR IDs: ADR-0002, ADR-0003
Specification references: JAV2I-14, JAV2I-14-RMI-IDL
Target module: modules/corba-rmi-iiop
Allowed files: docs/adr/**, docs/architecture/**, docs/requirements/functional-requirements.md, modules/corba-rmi-iiop/** after design approval
Forbidden files: RMI-IIOP implementation before dedicated compatibility ADR approval
Expected behavior: Task type: human-gate-blocked. Draft and approve the dedicated RMI-IIOP and Java-to-IDL compatibility design before implementation.
Tests to add/update: No implementation tests until the ADR approves scope; define future compatibility and interop scenarios.
Documentation to update: Dedicated ADR, architecture design, requirements, and conformance plan.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Implementation remains blocked until a dedicated ADR is accepted.
Rollback notes: Revert RMI-IIOP planning docs together.

