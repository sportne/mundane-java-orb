# G8-140 Time Service Conformance Closure

Task ID: G8-140-TIME-SERVICE-CONFORMANCE-CLOSURE
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-060, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0017
Specification references: TIME-11
Target module: modules/corba-time-service, modules/corba-native-image, interop harness, verification docs
Allowed files: modules/corba-time-service/src/**, modules/corba-time-service/README.md, modules/corba-native-image/src/**, modules/corba-native-image/build.gradle, modules/corba-interop-testkit/src/**, interop/bin/interop-peer, interop/peers/*/peer.yaml, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-140-time-service-conformance-closure.md, README.md
Forbidden files: unrelated service implementation, unapproved live peer execution, peer artifacts, Docker layers, committed raw live reports, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Close the Time Service conformance record after local behavior, IIOP/Naming exposure, Native Image smoke, structured interop metadata, and any approved live evidence are complete.
Tests to add/update: Run full Time Service unit, loopback, Native Image, interop metadata, and approved live peer validation; add final regression tests for any closure defects.
Documentation to update: Services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and module README.
Commands to run: ./gradlew :modules:corba-time-service:test :modules:corba-native-image:test :modules:corba-interop-testkit:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; ./interop/bin/interop-peer validate-manifests; git diff --check
Acceptance criteria: Time Service conformance matrix reflects the implemented subset and exclusions; all approved local and live evidence is summarized without raw artifacts; no unsupported optional-service claims are made.
Rollback notes: Revert closure docs, tests, harness changes, and roadmap status changes together.
