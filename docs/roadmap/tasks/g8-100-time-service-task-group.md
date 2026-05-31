# G8-100 Time Service Task Group

Task ID: G8-100-TIME-SERVICE-TASK-GROUP
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-060, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0017
Specification references: TIME-11
Target module: modules/corba-time-service, modules/corba-services-core
Allowed files: modules/corba-time-service/src/**, modules/corba-time-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, modules/corba-native-image/src/**, modules/corba-interop-testkit/src/**, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-100-time-service-task-group.md, README.md
Forbidden files: unrelated service implementation, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated artifacts outside explicit later tasks, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Blocked until maintainers promote a Time Service slice. Implement in staged steps: value/clock model, local service behavior, optional IIOP/Naming exposure, Native Image smoke, structured interop metadata, and conformance closure.
Tests to add/update: Add focused unit tests per promoted slice; add Native Image smoke after public entrypoints exist; add interop metadata tests before peer scenarios are claimed.
Documentation to update: Services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and module READMEs.
Commands to run: ./gradlew :modules:corba-time-service:test :modules:corba-services-core:test :modules:corba-native-image:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Time Service implementation remains split into narrow promoted slices; no live peer claim is made before metadata and missing-prerequisite reporting exist; Native Image restrictions from ADR-0017 are preserved.
Rollback notes: Revert Time Service implementation slices, tests, docs, and roadmap status changes together.
