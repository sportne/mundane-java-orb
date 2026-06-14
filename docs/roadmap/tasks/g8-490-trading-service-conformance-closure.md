# G8-490 Trading Service Conformance Closure

Task ID: G8-490-TRADING-SERVICE-CONFORMANCE-CLOSURE
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-010, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0020
Specification references: TRADE-10
Target module: modules/corba-trading-service
Allowed files: modules/corba-trading-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-490-trading-service-conformance-closure.md, docs/roadmap/tasks/g8-500-transaction-service-task-group.md, README.md
Forbidden files: runtime implementation changes, public API implementation changes, live peer execution, committed live interop reports, peer artifacts, Docker layers, prepared peer images, generated OMG APIs, durable persistence, remote federation execution, Native Image binary commits, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Close the Trading Service local conformance record across type repository behavior, offer repository behavior, bounded constraints, local query, import/export boundary metadata, loopback IIOP/Naming exposure, Native Image smoke, and interop dry-run metadata. Do not add runtime behavior or claim live peer pass/fail results.
Tests to add/update: No new runtime tests unless documentation verification reveals a missing assertion from earlier slices. Confirm existing Trading Service, Native Image, and interop metadata tests cover the closed record.
Documentation to update: Trading Service README, services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and next ready roadmap task if required by design-control gates.
Commands to run: ./gradlew :modules:corba-trading-service:test :modules:corba-services-core:test :modules:corba-native-image:test :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer run-direction-matrix --dry-run trading-service all; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Trading Service conformance documentation matches implemented local/IIOP/Native Image/dry-run behavior, live peer claims remain unapproved, and the next existing roadmap task is promoted only if design-control rules require a ready successor.
Rollback notes: Revert Trading Service conformance documentation and roadmap status updates together.
