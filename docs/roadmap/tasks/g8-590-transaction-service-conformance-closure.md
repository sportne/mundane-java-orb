# G8-590 Transaction Service Conformance Closure

Task ID: G8-590-TRANSACTION-SERVICE-CONFORMANCE-CLOSURE
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-040, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0021
Specification references: TRANS-14
Target module: modules/corba-transaction-service
Allowed files: modules/corba-transaction-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/requirements/service-requirements.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-590-transaction-service-conformance-closure.md, docs/roadmap/tasks/g8-600-security-service-task-group.md, README.md
Forbidden files: runtime implementation changes, public API implementation changes, live peer execution, committed live interop reports, peer artifacts, Docker layers, prepared peer images, generated OMG APIs, durable recovery logs, XA integration, Security Service integration, distributed peer two-phase commit claims, Native Image binary commits, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Close the Transaction Service local conformance record across coordinator/resource behavior, timeout policy, local state transitions, propagation metadata, recovery boundary, loopback IIOP request-context boundary, Native Image smoke, and interop dry-run metadata. Do not add runtime behavior or claim live peer pass/fail results, durable recovery logs, XA integration, Security Service integration, or distributed peer two-phase commit.
Tests to add/update: No new runtime tests unless documentation verification reveals a missing assertion from earlier slices. Confirm existing Transaction Service, Native Image, and interop metadata tests cover the closed record.
Documentation to update: Transaction Service README, services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and next ready roadmap task if required by design-control gates.
Commands to run: ./gradlew :modules:corba-transaction-service:test :modules:corba-services-core:test :modules:corba-native-image:test :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer run-direction-matrix --dry-run transaction-service all; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Transaction Service conformance documentation matches implemented local/IIOP/Native Image/dry-run behavior; live peer claims, durable recovery logs, XA integration, Security Service integration, and distributed peer two-phase commit remain unapproved; the next existing roadmap task is promoted only if design-control rules require a ready successor.
Rollback notes: Revert Transaction Service conformance documentation and roadmap status updates together.

Completion notes: Closed the Transaction Service / OTS conformance record for the implemented TRANS-14 subset across coordinator/resource behavior, timeout policy, local state transitions, propagation metadata, disabled durable-recovery boundary, loopback IIOP request-context boundary, Native Image smoke, and dry-run interop metadata. Live peer execution, durable recovery logs, XA integration, Security Service integration, and distributed peer two-phase commit remain unapproved. G8-600 is promoted as the next ready task group.
