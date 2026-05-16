# Coding-agent governance

This file is the binding operating contract for coding agents working in this repository.

## Mandatory reading before any change

Read these files before editing:

1. `docs/charter.md`
2. `docs/scope.md`
3. `docs/architecture/overview.md`
4. `docs/architecture/module-boundaries.md`
5. `docs/requirements/requirements-index.md`
6. `docs/adr/ADR-0001-design-first-process.md`
7. `docs/roadmap/roadmap-index.md`
8. `docs/build/README.md`

## Absolute rules

1. Do not implement CORBA runtime, protocol, IDL, ORB, POA, service, or compiler behavior outside a roadmap task whose status permits implementation.
2. Every implementation task must cite requirement IDs, specification references, target module, allowed files, forbidden files, tests, documentation updates, and an acceptance command.
3. Runtime modules must remain Native Image friendly by default.
4. Do not weaken Checkstyle, Spotless, SpotBugs, Error Prone, ArchUnit, JaCoCo, dependency verification, dependency locking, offline build, or Native Image policies without an ADR.
5. Do not introduce reflection, runtime bytecode generation, dynamic proxies, Java serialization for marshaling, `Unsafe`, `sun.*`, or `jdk.internal.*` in core modules without an ADR waiver.
6. Do not copy code from JacORB, GlassFish ORB, JBoss/OpenJDK ORB, ACE/TAO, OpenJDK, or any other reference implementation.
7. Do not silently ignore failed tests or skipped quality gates.

## Change scope standard

Make the smallest coherent change that satisfies the approved task. Keep edits focused on the requested behavior, documentation, build rule, or policy change.

- Do not perform opportunistic refactors, renames, formatting sweeps, dependency upgrades, or documentation rewrites unless they are required to complete the task.
- Preserve existing requirements, engineering plans, verification plans, validation plans, ADRs, and traceability records unless the task explicitly changes them.
- When a cleanup is useful but not necessary, document it as follow-up work instead of mixing it into the current change.

## Build-file documentation standard

Gradle files are contributor-facing documentation, not just machine instructions.

When editing `settings.gradle`, `build.gradle`, `gradle.properties`, `build-logic/**/*.gradle`, or module/example `build.gradle` files:

- Write comments for a recent high school graduate who is comfortable with computers but new to Gradle.
- Explain what each plugin, property, task, dependency block, publication block, repository choice, and non-obvious Gradle API call is doing.
- Prefer short comments next to the thing being explained.
- Explain why a project-specific convention exists, especially for offline builds, dependency verification, dependency locking, Java toolchains, coverage, publishing, and Native Image.
- Do not remove explanatory comments just because the Gradle code feels obvious to an experienced build engineer.

## Documentation consistency standard

Documentation is part of the project contract. When editing docs, build files, module layout, workflows, tools, or agent policy:

- Keep `README.md`, `CONTRIBUTING.md`, `docs/build/README.md`, `docs/architecture/build-architecture.md`, module READMEs, and roadmap task docs consistent with the actual repository layout.
- Verify task names, Gradle properties, module names, paths, and workflow names against the files that define them before documenting them.
- Update indexes when moving or replacing documentation.
- Prefer one canonical document for each topic; remove obsolete compatibility documents while the project is pre-1.0 unless a task explicitly requires a transition file.
- Run `./gradlew validateDesignControlPack qualityGate` and `git diff --check` after documentation changes that affect build, layout, or agent governance.

## Pre-1.0 compatibility policy

This project has not reached version 1.0, so agents do not need to preserve backwards compatibility when a breaking change makes the design, build, API, module layout, documentation, or contributor workflow clearer.

- Prefer the cleaner long-term shape over compatibility shims, duplicate entry points, or legacy aliases while the project is pre-1.0.
- If a pre-1.0 breaking change removes or renames a public task, property, module, package, artifact, file path, or documented workflow, update every affected document and example in the same change.
- After version 1.0, breaking changes must be reserved for the next major version and documented through the normal requirement and ADR process.

## Required task shape

Use `docs/roadmap/implementation-task-template.md` for implementation tasks.

## Required pull-request checklist

```text
[ ] Requirements referenced
[ ] ADR impact reviewed
[ ] Module boundaries respected
[ ] No forbidden runtime dependency added
[ ] No forbidden runtime reflection or dynamic generation added
[ ] Reference implementation policy respected
[ ] Unit tests added/updated where behavior changed
[ ] Interop/conformance docs updated if scope changed
[ ] ArchUnit rules pass or staged exceptions are documented
[ ] Static analysis passes or staged exceptions are documented
[ ] Coverage thresholds pass or staged exceptions are documented
[ ] Native-image lane considered
[ ] Docs and traceability updated if scope changed
```

## Escalation triggers

Open or update an ADR before proceeding if a task needs to:

- change standards baseline
- alter compatibility profiles
- add runtime-visible dependencies
- change IDL, protocol, ORB, POA, or service architecture
- add reflection, bytecode generation, dynamic proxies, serialization marshaling, `Unsafe`, `sun.*`, or `jdk.internal.*`
- change reference implementation policy
- change Native Image policy
- weaken any quality gate
