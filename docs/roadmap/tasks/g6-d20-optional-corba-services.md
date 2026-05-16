# G6-D20 Optional CORBA Services

Task ID: G6-D20-OPTIONAL-CORBA-SERVICES
Gate: Deferred service gate
Requirement IDs: REQ-SVC-001
ADR IDs: ADR-0002, ADR-0004, ADR-0005
Specification references: Service-specific references TBD by dedicated service ADR.
Target module: modules/corba-services-core and optional service modules
Allowed files: docs/adr/**, docs/architecture/services-design.md, modules/corba-services-core/** after service design approval
Forbidden files: Trading, Event, Notification, Transaction, Security, or Time service implementation before dedicated design approval
Expected behavior: Task type: human-gate-blocked. Create separate design, requirement, interop, and security reviews for each optional CORBA service.
Tests to add/update: No service implementation tests until each service gate is approved.
Documentation to update: Service-specific ADRs, requirements, conformance plans, and security review notes.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Optional service work is split into separately approved, traceable tasks.
Rollback notes: Revert optional-service planning docs together.

