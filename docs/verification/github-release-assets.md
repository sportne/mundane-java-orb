# GitHub Release Assets Verification

G11-010 adds the release publication path for `1.0.0`. The release target
publishes GitHub Release assets only. It intentionally does not configure
GitHub Packages, Maven Central, or any other remote Maven repository.

## Release contract

- The release workflow is `.github/workflows/release.yml`.
- The workflow runs on `v*` tag pushes and manual dispatch with an explicit tag.
- The tag must use `vMAJOR.MINOR.PATCH` format and match `corba.version`.
- `corba.version` must not contain `SNAPSHOT`.
- The workflow checks out the exact tag before building release assets.
- Workflow permissions are limited to `contents: write`; there is no
  `packages: write` permission.

## Asset contents

The workflow runs the existing local release validation tasks, then packages the
staged Maven repository from `build/staging-repository` as a GitHub Release
asset. The generated assets are:

- `mundane-java-orb-<version>-maven-repository.tar.gz`
- `mundane-java-orb-<version>-published-artifacts.txt`
- `mundane-java-orb-<version>-SHA256SUMS.txt`

The staged repository is produced by `offlineReleaseValidation` through the
existing local `stageReleasePublications` and publication dry-run checks. The
workflow never commits staged artifacts, raw build outputs, or generated
release assets.

## Required validation

The workflow must pass:

```bash
./gradlew test
./gradlew validateDesignControlPack qualityGate
./gradlew offlineReleaseValidation
```

Local task completion additionally runs:

```bash
git diff --check
```

## Security and distribution posture

The release path uses the repository-scoped GitHub token only to create a
GitHub Release and upload assets. It does not receive package-registry write
permissions, does not add Maven deployment repositories, and does not download
or publish peer interoperability artifacts.
