# G8-380 Notification Service Interop Metadata

Task ID: G8-380-NOTIFICATION-SERVICE-INTEROP-METADATA
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-030, REQ-INTEROP-009, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0019
Specification references: NOT-11, EVNT-12
Target module: modules/corba-interop-testkit, interop harness, modules/corba-notification-service
Allowed files: modules/corba-interop-testkit/src/**, modules/corba-notification-service/README.md, interop/bin/interop-peer, interop/idl/notification-service.idl, interop/peers/*/peer.yaml, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-380-notification-service-interop-metadata.md, docs/roadmap/tasks/g8-390-notification-service-conformance-closure.md, README.md
Forbidden files: live peer execution, committed live interop reports, peer artifacts, Docker layers, generated OMG compatibility APIs, broad protocol/runtime changes, persistent storage, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add `notification-service` as a report-only optional-service scenario with an IDL fixture for the supported NOT-11 subset, approved-peer manifest metadata, dry-run direction enumeration for JVM/native local lanes, `InteropScenario.notificationService()` identity, and deterministic missing-prerequisite reports for `--require-live`. Do not start peer containers or local live lanes for this scenario.
Tests to add/update: Add interop testkit/harness coverage for fixture presence, scenario identity, manifest declarations, `all` target filtering, dry-run non-mutation, missing live approval, missing local commands/binaries, missing cache/base image/container/peer image reports, report JSON fields, and ignored raw evidence paths.
Documentation to update: Notification README, services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and G8-390 status.
Commands to run: ./gradlew :modules:corba-interop-testkit:test :modules:corba-notification-service:test; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer run-direction-matrix --dry-run notification-service all; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Notification Service is discoverable in interop metadata and report-only prerequisite reporting; dry-run is deterministic and non-mutating; no live peer execution or pass/fail compatibility claim is added; G8-390 is promoted after completion.
Rollback notes: Revert interop metadata, fixture, manifest, harness/testkit, docs, and roadmap status changes together.
Completion evidence: Added `notification-service` peer metadata for approved peers, `interop/idl/notification-service.idl`, `InteropScenario.notificationService()`, dry-run matrix and `all` target coverage, and deterministic missing-prerequisite reports for live approval, scenario IDL, local JVM/native client/server inputs, artifact cache, digest-pinned base image, container runtime, peer image, and unapproved live execution. No peer container, local live lane, live report, or pass/fail compatibility claim is introduced.
