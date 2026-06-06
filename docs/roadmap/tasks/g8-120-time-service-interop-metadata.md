# G8-120 Time Service Interop Metadata

Task ID: G8-120-TIME-SERVICE-INTEROP-METADATA
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-060, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0017
Specification references: TIME-11
Target module: modules/corba-time-service, modules/corba-interop-testkit
Allowed files: modules/corba-time-service/src/**, modules/corba-time-service/README.md, modules/corba-interop-testkit/src/**, interop/bin/interop-peer, interop/idl/time-service.idl, interop/peers/*/peer.yaml, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-120-time-service-interop-metadata.md, docs/roadmap/tasks/g8-130-time-service-live-peer-gate.md, README.md
Forbidden files: live peer execution, committed live interop reports, peer artifacts, Docker layers, production runtime changes outside Time Service exposure, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add Time Service scenario metadata, dry-run matrix enumeration, and deterministic missing-prerequisite reports without running live peers.
Tests to add/update: Add interop-testkit and harness tests for scenario validation, peer filtering, JVM/native local runtime prerequisites, missing artifact cache/image/container runtime/live approval, and ignored raw evidence paths.
Documentation to update: Services design, optional services conformance/review, interop matrix, roadmap index, README, and peer manifest notes where needed.
Commands to run: ./gradlew :modules:corba-interop-testkit:test :modules:corba-time-service:test; ./interop/bin/interop-peer validate-manifests; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Time Service peer scenarios are discoverable and validate in dry-run mode; missing live prerequisites produce structured reports; G8-130 is promoted only as a decision-record task and no live peer execution is performed.
Completion evidence: Added `interop/idl/time-service.idl`, Time Service scenario metadata for JacORB, GlassFish CORBA ORB, JBoss OpenJDK ORB, and ACE/TAO, dry-run direction matrix coverage, local-server prerequisite reports, and structured classification vocabulary. `G8-130-TIME-SERVICE-LIVE-PEER-GATE` is ready to record the 2026-06-06 maintainer approval before any live Time Service peer run.
Rollback notes: Revert Time Service interop metadata, harness tests, docs, and roadmap status changes together.
