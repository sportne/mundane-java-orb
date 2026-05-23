# G8-D50 Security Service Design Gate

Task ID: G8-D50-SECURITY-SERVICE-DESIGN-GATE
Status: human-gate-blocked
Gate: Optional CORBA service approval
Requirement IDs: REQ-SVC-050, REQ-SVC-001
ADR IDs: ADR-0002, ADR-0004, ADR-0005
Specification references: SEC-18, CORBA-IOP-SECURITY
Target module: modules/corba-security-service, modules/corba-services-core
Allowed files: docs/adr/**, docs/architecture/services-design.md, docs/requirements/service-requirements.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-d50-security-service-design-gate.md, modules/corba-security-service/README.md, modules/corba-services-core/README.md after human gate approval
Forbidden files: Security Service or CSIv2 runtime implementation, generated bindings, protocol behavior, public APIs, Gradle dependency or artifact changes, source files under modules/**/src/** before dedicated design approval
Expected behavior: Task type: human-gate-blocked. Approve the Security Service and CSIv2 design, credential and policy boundaries, interop lane, Native Image posture, and dedicated security review before any implementation task is created.
Tests to add/update: No product tests until a later approved implementation task names concrete Security Service behavior.
Documentation to update: Dedicated ADR or equivalent design record, service requirement detail, conformance row, interop posture, security review note, Native Image restrictions, module README, and roadmap successor tasks.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Maintainers explicitly approve the Security Service scope and produce follow-on ready tasks with narrow allowed files before implementation begins.
Rollback notes: Revert Security Service planning docs together.
