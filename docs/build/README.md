# Project setup and build infrastructure

This page is the quick orientation guide for contributors who need to understand how the project is built. The deeper architecture record lives in `docs/architecture/build-architecture.md`, and offline validation details live in `docs/verification/offline-build-validation.md`.

## Main tools

- Gradle 9.5.1 is the pinned build tool.
- The build uses the Groovy DSL, so build files end in `.gradle`.
- Java compilation targets Java 21 by default through Gradle toolchains.
- CI also keeps a Java 25 compatibility lane.
- Shared dependency and plugin versions live in `gradle/libs.versions.toml`.

See `docs/build/toolchain-matrix.md` for the JVM and Native Image lanes.

## Native Image tool discovery

Do not assume GraalVM Native Image is already on the shell `PATH`. Local agents
and contributors should first check SDKMAN-managed JDKs under
`$SDKMAN_CANDIDATES_DIR/java` or `$HOME/.sdkman/candidates/java`, choose the
needed GraalVM candidate, then run native checks with `JAVA_HOME` and `PATH`
set explicitly. For example:

```bash
JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.2-graalce" \
PATH="$JAVA_HOME/bin:$PATH" \
./gradlew :modules:corba-idl-semantics:nativeIdlSemanticsSmoke
```

If a native-image executable lives outside a JDK `bin` directory, set
`NATIVE_IMAGE=/path/to/native-image` explicitly. Falling back to a bare
`native-image` lookup is acceptable only after checking SDKMAN candidates and
the requested `JAVA_HOME`.

## Project layout

- `settings.gradle` lists every Gradle project and centralizes repositories.
- `build.gradle` at the root defines whole-repository tasks such as `qualityGate`.
- `build-logic/` is an included Gradle build that holds convention plugins.
- `modules/` contains publishable libraries, BOMs, and internal test modules.
- `examples/` contains non-published sample builds.
- `tools/` contains helper scripts for offline repository preparation and offline verification.

## Root tasks

- `./gradlew projects` shows the project tree.
- `./gradlew validateDesignControlPack` checks required governance and design docs.
- `./gradlew checkAll` runs each included project's `check` task.
- `./gradlew qualityGate` runs the normal local and CI gate.
- `./gradlew printPublishedArtifacts` prints the planned Maven coordinates.
- `./gradlew printOfflineBuildInstructions` prints the offline build command pattern.

`prepareOfflineRepository` is kept as an older compatibility alias that points contributors to the current offline instructions.

## Convention plugins

Most module build files stay short because they apply convention plugins from `build-logic/src/main/groovy`.

- `corba.identity-conventions` sets group/version inheritance, reproducible archives, and dependency locking.
- `corba.java-library-conventions` sets Java 21 toolchains, JUnit, Error Prone, UTF-8, and test defaults.
- `corba.quality-conventions` configures Spotless, Checkstyle, and SpotBugs.
- `corba.coverage-conventions` configures JaCoCo reports and coverage verification.
- `corba.publishing-conventions` configures Maven publication for Java libraries.
- `corba.platform-conventions` configures Java platform BOM publication.
- `corba.application-conventions` configures application modules.
- `corba.native-image-conventions` configures GraalVM Native Image checks.
- `corba.architecture-conventions` configures architecture-test modules.
- `corba.docs-validation-conventions` adds design-control validation tasks.
- `corba.offline-conventions` adds offline build instructions.

## Build properties

- `corba.version` controls the Maven version for published artifacts.
- `corba.javaRelease` controls the Java release used by compile tasks.
- `corba.primaryProfile` names the default CORBA compatibility profile.
- `corba.offlineRepo` points Gradle at a prepared local Maven repository for offline builds.
- `corbaJavaRelease`, `corbaPrimaryCorbaProfile`, and `corbaOfflineRepo` are temporary legacy aliases.
- `org.gradle.dependency.verification=strict` requires dependency verification metadata to match.

## Dependency policy

Dependency versions are locked with Gradle lockfiles. Dependency verification metadata lives in `gradle/verification-metadata.xml`. When dependencies or plugins change, update both the locks and verification metadata deliberately and review the diff.

Dependabot updates are grouped into monthly batch PRs on the 15th with a 90-day cooldown.

## Offline builds

Use `tools/prepare-offline-repository.sh` to prepare a local Maven repository, then verify it with `tools/verify-offline-build.sh`. See `docs/build/offline-build.md` for the contributor workflow.
