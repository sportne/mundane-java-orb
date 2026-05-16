# Agent Handoff

This repository is in a gated scaffold phase. Implementation work requires an approved task with requirement IDs, specification references, target module, allowed and forbidden files, expected tests, documentation updates, and an exact acceptance command.

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

- Exact specification clause IDs have G1 section-level references in
  `docs/specification-traceability.md`, requirement tables, and conformance
  matrices. Feature handoffs must narrow these to task-specific clauses.
- External ORB peer scripts are placeholders.
- JaCoCo thresholds should tighten after empty-module validation.
- ArchUnit rules and interop peer container manifests still need expansion.
- The current G1 traceability task handoff is
  `docs/agent/g1-specification-traceability-handoff.md`.

## Implementation unlock

Implementation remains forbidden until gates G0 through G5 are approved and a task-specific handoff exists.
