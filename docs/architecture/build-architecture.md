# Build Architecture

Contributor-facing setup instructions live in `docs/build/README.md`. This file records the build architecture the project is expected to keep.

## Build system

- Gradle 9.5.1, pinned wrapper version.
- Groovy DSL.
- Version catalog.
- Convention plugins in `build-logic`.
- Dependency locking and verification metadata.
- Offline build support through `-Pcorba.offlineRepo=/path/to/repo`.
- Top-level `examples/` for non-published example builds.

## Repository layout

- Root `settings.gradle` includes `build-logic` through `pluginManagement`.
- Root `settings.gradle` centralizes repositories and uses `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
- Published libraries, the BOM, and internal test modules live under `modules/`.
- Non-published examples live under top-level `examples/`.
- Shared build behavior lives in composable convention plugins under `build-logic/src/main/groovy`.
- Helper scripts live under `tools/`.

## Public Gradle interface

- `validateDesignControlPack` verifies required design-control files and the
  roadmap task template/status model.
- `checkAll` runs checks for every included project.
- `qualityGate` is the local and CI quality gate.
- `printPublishedArtifacts` prints planned Maven coordinates.
- `printOfflineBuildInstructions` explains the offline build path.
- `prepareOfflineRepository` remains as a compatibility alias for older docs and scripts.

## Build properties

- `corba.version` controls published artifact versions.
- `corba.javaRelease` controls Java compile release and toolchain selection.
- `corba.primaryProfile` names the default CORBA compatibility profile.
- `corba.offlineRepo` selects a local Maven repository for offline dependency resolution.
- `corbaJavaRelease`, `corbaPrimaryCorbaProfile`, and `corbaOfflineRepo` are temporary legacy aliases.

## Quality tools

- JUnit Platform/Jupiter.
- ArchUnit.
- JaCoCo.
- Spotless.
- Checkstyle.
- SpotBugs.
- Error Prone.
- GraalVM Native Build Tools.

## Build matrix

- OpenJDK 21 JVM.
- OpenJDK 25 JVM.
- GraalVM JDK 21 JVM.
- GraalVM JDK 25 JVM.
- GraalVM JDK 21 Native Image.
- GraalVM JDK 25 Native Image.

## Dependency and update policy

Dependency verification runs in strict mode. Dependency lockfiles and `gradle/verification-metadata.xml` must be updated together when build dependencies change.

Dependabot updates are grouped into monthly batch PRs on the 15th with a 90-day cooldown.
