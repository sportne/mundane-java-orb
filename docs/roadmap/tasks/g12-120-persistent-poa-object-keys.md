# G12-120 Persistent POA Object Keys

Task ID: G12-120-PERSISTENT-POA-OBJECT-KEYS
Status: ready-for-implementation
Gate: G12 post-1.0 runtime identity implementation
Requirement IDs: REQ-POA-001, REQ-POA-002, REQ-ORB-001, REQ-IOR-001, REQ-SEC-006, REQ-NATIVE-002
ADR IDs: ADR-0001, ADR-0005, ADR-0008, ADR-0010, ADR-0014
Specification references: CORBA-IF-POA, CORBA-IF-OBJECT-REF, CORBA-IOP-IOR
Target module: modules/corba-poa, modules/corba-orb-core
Allowed files: modules/corba-poa/src/**, modules/corba-orb-core/src/**, modules/corba-native-image/src/**, docs/architecture/poa-design.md, docs/architecture/runtime-architecture.md, docs/conformance/corba-3.4-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g12-120-persistent-poa-object-keys.md, docs/roadmap/tasks/g12-130-persistent-ior-roundtrip.md, README.md
Forbidden files: peer artifacts, live interop reports, optional service implementation, Naming persistence, Java serialization metadata, reflection metadata, runtime bytecode generation, dynamic proxies, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Blocked until G12-110 completes. Enable persistent POA identity and restart-safe object keys for retained USER_ID/SYSTEM_ID activations without adding persistent IOR round-trip claims yet.
Tests to add/update: POA policy tests for persistent lifespan acceptance, object-key stability, restart simulation using configured ORB/POA ids, stale-key diagnostics, and hostile object-id bounds.
Documentation to update: POA design, runtime architecture, CORBA conformance matrix, roadmap index, README ready-task status, this task, and G12-130 status when complete.
Commands to run: ./gradlew :modules:corba-poa:test :modules:corba-orb-core:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Persistent POA activations produce stable durable object keys under configured ORB identity; malformed and stale keys fail deterministically; G12-130 is promoted to ready-for-implementation.
Rollback notes: Revert POA persistent-key runtime, tests, Native Image, and documentation updates together.
