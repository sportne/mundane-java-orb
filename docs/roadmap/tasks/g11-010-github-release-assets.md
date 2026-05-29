# G11-010 GitHub Release Assets

Task ID: G11-010-GITHUB-RELEASE-ASSETS
Status: complete
Gate: G11 1.0.0 release publication
Requirement IDs: REQ-NFR-008, REQ-OFFLINE-001, REQ-OFFLINE-002, REQ-OFFLINE-003, REQ-INTEROP-009
ADR IDs: ADR-0011, ADR-0012, ADR-0013
Specification references: Release publication policy; no direct OMG clause.
Target module: GitHub release workflow and release documentation
Allowed files: .github/workflows/release.yml, gradle.properties, docs/releases/1.0.0.md, docs/verification/github-release-assets.md, docs/verification/verification-index.md, docs/build/README.md, docs/architecture/build-architecture.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g11-010-github-release-assets.md, README.md
Forbidden files: production source changes, test source changes, generated artifacts, published binary outputs, remote Maven repository configuration, GitHub Packages publishing, Maven Central publishing, optional service implementation
Expected behavior: Task type: implementation. Set the project version to 1.0.0 and add a GitHub Actions release workflow that validates the repository, stages Maven publications locally, and uploads the staged repository archive plus checksums and publication summary as GitHub Release assets only.
Tests to add/update: No product tests. Release behavior is covered by existing repository tests, design-control validation, quality gates, and offline release validation.
Documentation to update: README, build docs, build architecture, release notes, verification index, release asset verification record, and roadmap index.
Commands to run: ./gradlew test; ./gradlew validateDesignControlPack qualityGate; ./gradlew offlineReleaseValidation; git diff --check
Acceptance criteria: `corba.version` is 1.0.0; the release workflow requires tag/version agreement; the workflow has `contents: write` but no `packages: write`; the workflow publishes only GitHub Release assets; no remote Maven publishing is configured; optional CORBA Services remain human-gated.
Completion evidence: Completed on 2026-05-29. Local validation passed with `./gradlew test`, `./gradlew validateDesignControlPack qualityGate`, `./gradlew offlineReleaseValidation`, and `git diff --check`. The release workflow packages `build/staging-repository` into a deterministic archive, emits a publication summary and SHA-256 checksums, and creates a GitHub Release from tag `v1.0.0` using GitHub Release assets only.
Rollback notes: Revert the release workflow, version change, release notes, verification record, and roadmap/docs updates. Delete any draft GitHub Release assets manually if a release run was already executed.
