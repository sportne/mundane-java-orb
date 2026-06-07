# G8-260 Event Service Interop Metadata

Task ID: G8-260-EVENT-SERVICE-INTEROP-METADATA
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-020, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0018
Specification references: EVNT-12
Target module: modules/corba-event-service, modules/corba-interop-testkit, interop harness
Allowed files: modules/corba-event-service/src/**, modules/corba-event-service/README.md, modules/corba-interop-testkit/src/**, interop/bin/interop-peer, interop/idl/event-service.idl, interop/peers/*/peer.yaml, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-260-event-service-interop-metadata.md, docs/roadmap/tasks/g8-270-event-service-conformance-closure.md, README.md
Forbidden files: live peer execution, committed live interop reports, peer artifacts, Docker layers, production runtime changes outside Event Service metadata support, generated OMG compatibility APIs, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add Event Service scenario metadata, IDL fixture, dry-run direction matrix enumeration, and deterministic missing-prerequisite reports for approved peers without running live peers.
Tests to add/update: Add interop-testkit and harness tests for scenario validation, peer filtering, JVM/native local runtime prerequisites, missing artifact cache/image/container runtime/live approval, and existing ignored raw evidence paths.
Documentation to update: Services design, optional services conformance/review, interop matrix, roadmap index, README, and peer manifest notes where needed.
Commands to run: ./gradlew :modules:corba-interop-testkit:test :modules:corba-event-service:test; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer run-direction-matrix --dry-run event-service all; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Event Service peer scenarios are discoverable and validate in dry-run mode; missing live prerequisites produce structured reports; G8-270 is promoted for local conformance closure and no live peer execution is performed.
Rollback notes: Revert Event Service interop metadata, harness tests, docs, and roadmap status changes together.
