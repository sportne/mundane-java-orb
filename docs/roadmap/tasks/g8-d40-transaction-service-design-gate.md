# G8-D40 Transaction Service Design Gate

Task ID: G8-D40-TRANSACTION-SERVICE-DESIGN-GATE
Status: complete
Gate: Optional CORBA service approval
Requirement IDs: REQ-SVC-040, REQ-SVC-001
ADR IDs: ADR-0002, ADR-0004, ADR-0005, ADR-0016, ADR-0021
Specification references: TRANS-14
Target module: modules/corba-transaction-service, modules/corba-services-core
Allowed files: docs/adr/**, docs/architecture/services-design.md, docs/requirements/service-requirements.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-d40-transaction-service-design-gate.md, docs/roadmap/tasks/g8-500-transaction-service-task-group.md, modules/corba-transaction-service/README.md, modules/corba-services-core/README.md after human gate approval
Forbidden files: Transaction Service or OTS runtime implementation, generated bindings, protocol behavior, public APIs, Gradle dependency or artifact changes, source files under modules/**/src/** before dedicated design approval
Expected behavior: Task type: human-gate-blocked. Approve the Transaction Service design, OTS compatibility slice, resource/coordination boundary, interop lane, Native Image posture, and security review before any implementation task is created.
Tests to add/update: No product tests until a later approved implementation task names concrete Transaction Service behavior.
Documentation to update: Dedicated ADR or equivalent design record, service requirement detail, conformance row, interop posture, security review note, Native Image restrictions, module README, and roadmap successor tasks.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Maintainers explicitly approve the Transaction Service scope and produce follow-on ready tasks with narrow allowed files before implementation begins.
Completion evidence: Completed on 2026-05-31 by G8 optional services look-back approval. ADR-0016 and ADR-0021 define the staged Transaction Service / OTS design; G8-500 records the blocked follow-on task group. No runtime behavior is approved by this gate completion.
Rollback notes: Revert Transaction Service planning docs together.
