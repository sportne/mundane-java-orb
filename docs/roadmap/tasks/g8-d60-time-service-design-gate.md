# G8-D60 Time Service Design Gate

Task ID: G8-D60-TIME-SERVICE-DESIGN-GATE
Status: human-gate-blocked
Gate: Optional CORBA service approval
Requirement IDs: REQ-SVC-060, REQ-SVC-001
ADR IDs: ADR-0002, ADR-0004, ADR-0005
Specification references: TIME-11
Target module: modules/corba-time-service, modules/corba-services-core
Allowed files: docs/adr/**, docs/architecture/services-design.md, docs/requirements/service-requirements.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-d60-time-service-design-gate.md, modules/corba-time-service/README.md, modules/corba-services-core/README.md after human gate approval
Forbidden files: Time Service runtime implementation, generated bindings, protocol behavior, public APIs, Gradle dependency or artifact changes, source files under modules/**/src/** before dedicated design approval
Expected behavior: Task type: human-gate-blocked. Approve the Time Service design, clock/interval boundary, interop lane, Native Image posture, and security review before any implementation task is created.
Tests to add/update: No product tests until a later approved implementation task names concrete Time Service behavior.
Documentation to update: Dedicated ADR or equivalent design record, service requirement detail, conformance row, interop posture, security review note, Native Image restrictions, module README, and roadmap successor tasks.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Maintainers explicitly approve the Time Service scope and produce follow-on ready tasks with narrow allowed files before implementation begins.
Rollback notes: Revert Time Service planning docs together.
