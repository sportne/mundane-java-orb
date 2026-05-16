# Offline Build Requirements

| ID | Requirement | Status | Trace reference |
|---|---|---|---|
| REQ-OFFLINE-001 | The project shall build with `--offline` when all dependencies and plugins are present in a supplied local Maven repository. | draft | ADR-0011, docs/build/offline-build.md |
| REQ-OFFLINE-002 | No dynamic dependency versions are allowed. | draft | ADR-0011, docs/build/offline-build.md |
| REQ-OFFLINE-003 | No SNAPSHOT dependencies are allowed in release builds. | draft | ADR-0011, docs/build/offline-build.md |
| REQ-OFFLINE-004 | All repositories shall be declared in `settings.gradle`. | draft | ADR-0011, docs/architecture/build-architecture.md |
| REQ-OFFLINE-005 | Dependency locking files and verification metadata shall be committed and refreshed when build dependencies change. | draft | ADR-0011, docs/build/offline-build.md |
| REQ-OFFLINE-006 | The Gradle wrapper distribution shall be mirrored or pre-provisioned for offline CI. | draft | ADR-0011, docs/build/offline-build.md |
