# G8-270 Event Service Conformance Closure

Task ID: G8-270-EVENT-SERVICE-CONFORMANCE-CLOSURE
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-020, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0018
Specification references: EVNT-12
Target module: modules/corba-event-service, modules/corba-native-image, interop harness, verification docs
Allowed files: modules/corba-event-service/src/**, modules/corba-event-service/README.md, modules/corba-native-image/src/**, modules/corba-interop-testkit/src/**, interop/bin/interop-peer, interop/idl/event-service.idl, interop/peers/*/peer.yaml, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-270-event-service-conformance-closure.md, docs/roadmap/tasks/g8-300-notification-service-task-group.md, README.md
Forbidden files: live peer execution, committed live interop reports, peer artifacts, Docker layers, unrelated service implementation, generated OMG compatibility APIs, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Close the Event Service local conformance record after local lifecycle, push/pull delivery, backpressure, loopback IIOP/Naming, Native Image smoke, and structured interop metadata are complete.
Tests to add/update: Run full Event Service unit, loopback, Native Image, and interop metadata validation; add regression tests only for closure defects.
Documentation to update: Services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and module README.
Commands to run: ./gradlew :modules:corba-event-service:test :modules:corba-native-image:test :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer run-direction-matrix --dry-run event-service all; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Event Service conformance matrix reflects the implemented local/IIOP/Native Image/dry-run subset and exclusions; no live peer execution is run or claimed; Notification Service remains blocked unless its task contract explicitly allows promotion.
Rollback notes: Revert closure docs, tests, harness changes, and roadmap status changes together.
