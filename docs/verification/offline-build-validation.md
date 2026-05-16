# Offline Build Validation

Contributor-facing commands live in `docs/build/offline-build.md`.

## Goal

Validate that the project can be built without network access when supplied with
a complete local Maven repository and a pre-provisioned Gradle wrapper
distribution.

## Required command

```bash
./gradlew --offline -Pcorba.offlineRepo=/path/to/local-maven-repo clean qualityGate
```

## Release validation

Release validation must include:

- isolated Gradle user home;
- no network access;
- dependency verification enabled;
- dependency locks present;
- plugin dependencies present;
- publication dry run;
- sample downstream consumer build.
