# Agent Handoff

This repository is entering G6 from a gated scaffold phase. Implementation work
requires an approved task with requirement IDs, specification references, target
module, allowed and forbidden files, expected tests, documentation updates, and
an exact acceptance command.

## Current scaffold state

- Design-control documentation, requirement catalogs, ADRs, conformance matrices, and traceability structure are in place.
- Gradle 9.5.1, Groovy DSL, included `build-logic`, Java toolchains, quality plugins, dependency verification, dependency locking, offline build helpers, and CI placeholder workflows are scaffolded.
- The multi-artifact module layout, top-level examples layout, and interop lab manifests are present.

## Current infrastructure entry points

- `./gradlew projects`
- `./gradlew validateDesignControlPack`
- `./gradlew checkAll`
- `./gradlew qualityGate`
- `./gradlew printPublishedArtifacts`
- `./gradlew printOfflineBuildInstructions`

## Current setup references

- `docs/build/README.md`
- `docs/architecture/build-architecture.md`
- `docs/verification/offline-build-validation.md`

## Current open setup work

- G0 through G5 have been reviewed and approved by the maintainer.
- G6 starts with the roadmap task set captured in
  `docs/agent/g6-project-roadmap-handoff.md` and `docs/roadmap/`.
- G6-010 gate state/control and G6-030 common diagnostics/limits are captured in
  `docs/agent/g6-010-gate-state-and-control-handoff.md` and
  `docs/agent/g6-030-common-diagnostics-limits-handoff.md`.
- `docs/roadmap/tasks/g6-040-repository-id-foundation.md` is the next
  ready-for-handoff candidate.
- Remaining roadmap task files are drafts. A task must be narrowed into a
  task-specific G6 handoff before implementation begins.
- G5 validation readiness closure is captured in
  `docs/agent/g5-validation-gate-readiness-handoff.md` and
  `docs/verification/g5-validation-gate-readiness.md`.
- Exact specification clause IDs have G1 section-level references in
  `docs/specification-traceability.md`, requirement tables, and conformance
  matrices. Feature handoffs must narrow these to task-specific clauses.
- G3 interop peer manifests have been expanded in
  `docs/agent/g3-interop-peer-manifest-handoff.md`.
- G4 interop peer launch scaffolding has been added in
  `docs/agent/g4-interop-peer-launch-handoff.md`; later work must add real peer
  artifact resolution, process launch, and clean-room report capture.
- JaCoCo thresholds should tighten after empty-module validation.
- G2 ArchUnit boundary enforcement has been expanded in
  `docs/agent/g2-architecture-boundary-handoff.md`; empty-rule scaffold
  tolerance remains until later validation gates.
- The current G1 traceability task handoff is
  `docs/agent/g1-specification-traceability-handoff.md`.

## Implementation unlock

Implementation remains forbidden until a roadmap task is narrowed into an
approved task-specific G6 handoff.
