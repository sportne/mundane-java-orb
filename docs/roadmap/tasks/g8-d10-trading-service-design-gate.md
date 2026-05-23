# G8-D10 Trading Service Design Gate

Task ID: G8-D10-TRADING-SERVICE-DESIGN-GATE
Status: human-gate-blocked
Gate: Optional CORBA service approval
Requirement IDs: REQ-SVC-010, REQ-SVC-001
ADR IDs: ADR-0002, ADR-0004, ADR-0005
Specification references: TRADE-10
Target module: modules/corba-trading-service, modules/corba-services-core
Allowed files: docs/adr/**, docs/architecture/services-design.md, docs/requirements/service-requirements.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-d10-trading-service-design-gate.md, modules/corba-trading-service/README.md, modules/corba-services-core/README.md after human gate approval
Forbidden files: Trading Service runtime implementation, generated bindings, protocol behavior, public APIs, Gradle dependency or artifact changes, source files under modules/**/src/** before dedicated design approval
Expected behavior: Task type: human-gate-blocked. Approve the Trading Service design, compatibility slice, interop lane, Native Image posture, and security review before any implementation task is created.
Tests to add/update: No product tests until a later approved implementation task names concrete Trading Service behavior.
Documentation to update: Dedicated ADR or equivalent design record, service requirement detail, conformance row, interop posture, security review note, Native Image restrictions, module README, and roadmap successor tasks.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Maintainers explicitly approve the Trading Service scope and produce follow-on ready tasks with narrow allowed files before implementation begins.
Rollback notes: Revert Trading Service planning docs together.
