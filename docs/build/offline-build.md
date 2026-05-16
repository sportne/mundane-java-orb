# Offline build workflow

Offline builds are for environments where Gradle must not contact remote repositories.

## Inputs

- The committed Gradle wrapper files, including `gradle/wrapper/gradle-wrapper.jar`.
- A pre-provisioned Gradle distribution in the Gradle user home or an approved internal wrapper mirror.
- A local Maven-style repository containing all plugins and dependencies.
- Current Gradle dependency locks and `gradle/verification-metadata.xml`.

## Commands

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

## Rules

Do not add hidden network access to tests or build logic. Any test that needs a remote resource must be explicitly tagged as an integration-style test and documented before it is added.
