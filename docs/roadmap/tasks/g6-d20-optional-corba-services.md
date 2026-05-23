# G6-D20 Optional CORBA Services

Task ID: G6-D20-OPTIONAL-CORBA-SERVICES
Status: complete
Gate: Deferred service gate
Requirement IDs: REQ-SVC-001, REQ-SVC-010, REQ-SVC-020, REQ-SVC-030, REQ-SVC-040, REQ-SVC-050, REQ-SVC-060
ADR IDs: ADR-0002, ADR-0004, ADR-0005
Specification references: TRADE-10, EVNT-12, NOT-11, TRANS-14, SEC-18, TIME-11
Target module: docs-only service gate split; modules/corba-services-core and optional service modules remain scaffold-only
Allowed files: README.md, docs/specification-traceability.md, docs/standards-baseline.md, docs/requirements/requirements-index.md, docs/requirements/functional-requirements.md, docs/requirements/service-requirements.md, docs/architecture/services-design.md, docs/conformance/conformance-index.md, docs/conformance/optional-services-matrix.md, docs/verification/verification-index.md, docs/verification/interop-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-d20-optional-corba-services.md, docs/roadmap/tasks/g8-d*-service-design-gate.md
Forbidden files: Trading, Event, Notification, Transaction, Security, or Time service implementation; modules/** source changes; Gradle dependency, plugin, artifact coordinate, generated code, protocol, or runtime behavior changes
Expected behavior: Task type: human-gate-blocked. Create separate design, requirement, interop, and security reviews for each optional CORBA service while keeping all service implementation gates blocked.
Tests to add/update: No product tests. Add documentation-only conformance and verification records for the split service gates.
Documentation to update: Service spec keys, service requirements, service architecture boundaries, optional-service conformance, optional-service verification, roadmap index, split service gate tasks, top-level ready-task state, and this task contract.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Optional service work is split into separately approved, traceable tasks; each optional service has a requirement row, conformance row, interop posture, security-review note, and human-gated follow-on task; no non-human-gated ready task is introduced.
Rollback notes: Revert optional-service planning docs together.
