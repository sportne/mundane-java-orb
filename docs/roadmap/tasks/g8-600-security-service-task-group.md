# G8-600 Security Service Task Group

Task ID: G8-600-SECURITY-SERVICE-TASK-GROUP
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-050, REQ-IIOP-002, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0022
Specification references: SEC-18, CORBA-IOP-SECURITY
Target module: modules/corba-security-service, modules/corba-services-core, modules/corba-iiop
Allowed files: modules/corba-security-service/src/**, modules/corba-security-service/README.md, modules/corba-services-core/src/**, modules/corba-services-core/README.md, modules/corba-iiop/src/**, modules/corba-native-image/src/**, modules/corba-interop-testkit/src/**, docs/architecture/services-design.md, docs/architecture/cdr-giop-iiop.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-600-security-service-task-group.md, README.md
Forbidden files: unrelated service implementation, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated artifacts outside explicit later tasks, global JVM security-manager integration, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Split Security Service / CSIv2 into staged implementation slices: credential/trust model, policy model, CSIv2 metadata, local policy evaluation, audit/failure disclosure, IIOP integration, Native Image smoke, structured interop metadata, and conformance closure.
Tests to add/update: Add credential, policy, CSIv2 metadata, audit redaction, IIOP integration, Native Image, and interop metadata tests per promoted slice.
Documentation to update: Services design, CDR/GIOP/IIOP architecture, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and module READMEs.
Commands to run: ./gradlew :modules:corba-security-service:test :modules:corba-services-core:test :modules:corba-iiop:test :modules:corba-native-image:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Security Service implementation remains split into narrow promoted slices; audit and failure disclosure do not leak secrets; no live secure peer claim is added early.
Rollback notes: Revert Security Service implementation slices, tests, docs, and roadmap status changes together.
