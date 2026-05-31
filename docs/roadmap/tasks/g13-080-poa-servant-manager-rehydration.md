# G13-080 POA Servant Manager Rehydration

Task ID: G13-080-POA-SERVANT-MANAGER-REHYDRATION
Status: ready-for-implementation
Gate: G13 durable runtime hardening
Requirement IDs: REQ-ORB-001, REQ-POA-001, REQ-POA-002, REQ-IOR-001, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0005, ADR-0008, ADR-0010, ADR-0014, ADR-0015
Specification references: CORBA-IF-ORB, CORBA-IF-POA, CORBA-IF-OBJECT-REF
Target module: modules/corba-poa, modules/corba-orb-core, modules/corba-native-image
Allowed files: modules/corba-poa/src/**, modules/corba-orb-core/src/**, modules/corba-native-image/src/**, docs/architecture/runtime-architecture.md, docs/architecture/poa-design.md, docs/conformance/corba-3.4-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g13-080-poa-servant-manager-rehydration.md, docs/roadmap/tasks/g13-090-iiop-durable-key-poa-routing.md, README.md
Forbidden files: peer artifacts, committed live interop reports, interop cache files, optional service implementation, Java serialization metadata, reflection metadata, runtime bytecode generation, dynamic proxies, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Apply POA servant-manager policy to validated durable object ids after durable POA path lookup.
Tests to add/update: Add tests for retained `ServantActivator` incarnation, active-object-map reuse, `USE_ACTIVE_OBJECT_MAP_ONLY` non-rehydration, default-servant durable object-id delivery, servant-manager failure mapping, persistent `NON_RETAIN` decision coverage, hostile object ids, and Native Image smoke coverage.
Documentation to update: Runtime architecture, POA design, CORBA conformance matrix, Native Image matrix if touched, roadmap index, README ready-task status, this task, and G13-090 status when complete.
Commands to run: ./gradlew :modules:corba-poa:test :modules:corba-orb-core:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Valid durable object ids reach only the servant lookup path allowed by POA policy, retained servant activation records active entries deterministically, unsupported policy combinations fail explicitly, no servant persistence or Java serialization is introduced, and G13-090 is promoted only if maintainers approve continuing the implementation sequence.
Rollback notes: Revert servant-manager rehydration code, tests, docs, Native Image updates, and roadmap status changes together.
