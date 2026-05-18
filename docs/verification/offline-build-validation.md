# Offline Build Validation

Contributor-facing commands live in `docs/build/offline-build.md`.

## Goal

Validate that the project can be built without network access when supplied with
a complete local Maven repository and a pre-provisioned Gradle wrapper
distribution.

## Required command

```bash
./gradlew offlineReleaseValidation
./tools/prepare-offline-repository.sh build/local-maven-repo
./tools/verify-offline-build.sh build/local-maven-repo
./gradlew --offline -Pcorba.offlineRepo=/path/to/local-maven-repo clean qualityGate
```

## Release validation

Release validation must include:

- isolated Gradle user home seeded from pre-provisioned wrapper and cache state;
- no network access;
- dependency verification enabled;
- dependency locks present;
- plugin dependencies present;
- publication dry run;
- BOM alignment validation;
- deterministic artifact manifest checksums;
- sample downstream consumer build.

## G6-920 implemented checks

- `validateOfflineReleaseInputs` checks wrapper metadata, strict dependency
  verification, dependency lockfiles, dynamic-version policy, external SNAPSHOT
  dependency policy, and offline repository presence during offline runs.
- `validateBomAlignment` checks that `corba-bom` constrains each published module
  exactly once and has no stale project constraints.
- `stageReleasePublications` publishes every module artifact and the BOM to
  `build/staging-repository`.
- `validatePublicationDryRun` verifies staged Maven metadata, POMs, module
  metadata, main jars, sources jars, Javadoc jars, and BOM constraint metadata.
- `validateDownstreamSampleConsumer` builds `examples/offline-release-consumer`
  offline from the staged Maven repository without project substitution.
