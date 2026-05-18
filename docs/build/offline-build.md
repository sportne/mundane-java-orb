# Offline build workflow

Offline builds are for environments where Gradle must not contact remote repositories.

## Inputs

- The committed Gradle wrapper files, including `gradle/wrapper/gradle-wrapper.jar`.
- A pre-provisioned Gradle distribution in the Gradle user home or an approved internal wrapper mirror.
- A local Maven-style repository containing all plugins and dependencies.
- Current Gradle dependency locks and `gradle/verification-metadata.xml`.

## Commands

Run the local release validation aggregate:

```bash
./gradlew offlineReleaseValidation
```

Prepare a local repository:

```bash
./tools/prepare-offline-repository.sh /path/to/local-maven-repo
```

Verify the build can use it:

```bash
./tools/verify-offline-build.sh /path/to/local-maven-repo
```

The underlying Gradle pattern is:

```bash
./gradlew --offline -Pcorba.offlineRepo=/path/to/local-maven-repo clean qualityGate
```

The older property name `corbaOfflineRepo` is still accepted during the transition, but new docs and scripts should use `corba.offlineRepo`.

## Release validation flow

`offlineReleaseValidation` performs the source-controlled checks that do not
need a prebuilt external repository:

- validates strict dependency verification, wrapper metadata, lockfiles, and
  static dependency-version policy;
- verifies the BOM has one project constraint for every published module except
  the BOM itself;
- publishes every artifact to `build/staging-repository` and checks Maven
  layout, POMs, Gradle module metadata, main jars, source jars, Javadoc jars, and
  BOM metadata;
- builds `examples/offline-release-consumer` as a standalone Gradle build
  against the staged Maven repository.

`prepare-offline-repository.sh` copies external Gradle cache artifacts and the
local staging repository into the target Maven-layout directory, then writes
`MANIFEST.sha256`.

`verify-offline-build.sh` requires and validates `MANIFEST.sha256`, copies the
supplied repository into a temporary isolated Gradle user home, seeds that home
with the pre-provisioned wrapper, dependency, and build-cache state from the
caller's Gradle user home, and runs `clean qualityGate` with `--offline` and
strict dependency verification.

## Rules

Do not add hidden network access to tests or build logic. Any test that needs a remote resource must be explicitly tagged as an integration-style test and documented before it is added.
