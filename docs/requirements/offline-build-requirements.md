# Offline Build Requirements

| ID | Requirement | Status |
|---|---|---|
| REQ-OFFLINE-001 | The project shall build with `--offline` when all dependencies and plugins are present in a supplied local Maven repository. | draft |
| REQ-OFFLINE-002 | No dynamic dependency versions are allowed. | draft |
| REQ-OFFLINE-003 | No SNAPSHOT dependencies are allowed in release builds. | draft |
| REQ-OFFLINE-004 | All repositories shall be declared in `settings.gradle`. | draft |
| REQ-OFFLINE-005 | Dependency locking and verification metadata template shall be committed after G4 completion. | draft |
| REQ-OFFLINE-006 | The Gradle wrapper distribution shall be mirrored or pre-provisioned for offline CI. | draft |
