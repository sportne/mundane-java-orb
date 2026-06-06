# G8-130 Time Service Live Peer Gate

Task ID: G8-130-TIME-SERVICE-LIVE-PEER-GATE
Status: human-gate-blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-060, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0017
Specification references: TIME-11
Target module: interop harness and verification docs
Allowed files: modules/corba-time-service/src/**, modules/corba-interop-testkit/src/**, interop/bin/interop-peer, interop/peers/*/peer.yaml, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-130-time-service-live-peer-gate.md, docs/roadmap/tasks/g8-140-time-service-conformance-closure.md, README.md
Forbidden files: unapproved live peer execution, peer artifacts, Docker layers, committed raw live reports, unrelated service implementation, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: human-gate-blocked. Record maintainer approval or rejection for live Time Service peer execution after local IIOP/Naming exposure and structured prerequisite reports exist.
Tests to add/update: If approved, run only the approved prerequisite or dry-run commands named by maintainers; do not execute live peer scenarios in this gate unless explicitly approved.
Documentation to update: Optional services review, interop matrix, roadmap index, README, and reference-behavior capture if approval includes clean-room evidence rules.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Maintainer decision is recorded; if live execution is approved, G8-140 is unblocked or promoted according to that decision; raw peer evidence remains uncommitted.
Rollback notes: Revert the gate decision docs and roadmap status changes together.
