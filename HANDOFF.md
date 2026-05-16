# Repository Handoff

This repository is intended to be handed to coding agents after human review.

## What has been created

- Design-control documentation.
- Requirement templates and initial requirement catalogs.
- Compatibility profile strategy.
- Reference implementation policy.
- Specification traceability structure.
- Conformance matrix templates.
- ADRs for initial decisions.
- Gradle 9 Groovy DSL scaffold.
- Build logic convention plugins.
- JUnit Platform/Jupiter setup.
- ArchUnit setup.
- JaCoCo, Checkstyle, SpotBugs, Error Prone, and Spotless setup.
- Native Image plugin scaffold.
- Offline build support scaffold.
- Multi-artifact module layout.
- Interop lab manifests.
- CI workflow placeholders.

## What has not been done

- No CORBA implementation has started.
- Gradle wrapper JAR has not been generated in this artifact.
- Dependency locks and verification metadata template are placeholders to be completed in a connected environment.
- Exact specification clause IDs remain TBD.
- External ORB peer scripts are placeholders.

## First recommended follow-up tasks

1. Run `./tools/bootstrap-gradle-wrapper.sh` in a connected environment.
2. Run `./gradlew clean qualityGate`.
3. Generate dependency locks and verification metadata template.
4. Complete exact specification clause references.
5. Tighten JaCoCo thresholds after empty-module validation.
6. Expand ArchUnit rules.
7. Build the first interop peer container manifest.
8. Only then create first implementation task.
