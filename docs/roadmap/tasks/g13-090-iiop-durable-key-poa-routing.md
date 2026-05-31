# G13-090 IIOP Durable Key POA Routing

Task ID: G13-090-IIOP-DURABLE-KEY-POA-ROUTING
Status: complete
Gate: G13 durable runtime hardening
Requirement IDs: REQ-ORB-001, REQ-POA-001, REQ-POA-002, REQ-IOR-001, REQ-IOR-002, REQ-IIOP-001, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0005, ADR-0008, ADR-0010, ADR-0014, ADR-0015
Specification references: CORBA-IF-ORB, CORBA-IF-POA, CORBA-IF-OBJECT-REF, CORBA-IOP-IOR, CORBA-IOP-GIOP, CORBA-IOP-IIOP
Target module: modules/corba-iiop, modules/corba-orb-core, modules/corba-poa, modules/corba-native-image
Allowed files: modules/corba-iiop/src/**, modules/corba-orb-core/src/**, modules/corba-poa/src/**, modules/corba-native-image/src/**, docs/architecture/runtime-architecture.md, docs/architecture/poa-design.md, docs/architecture/cdr-giop-iiop.md, docs/conformance/corba-3.4-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g13-090-iiop-durable-key-poa-routing.md, README.md
Forbidden files: peer artifacts, committed live interop reports, interop cache files, optional service implementation, Java serialization metadata, reflection metadata, runtime bytecode generation, dynamic proxies, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Route opaque persistent IIOP object keys into ORB/POA durable rehydration lookup without making protocol modules parse `MJOK`.
Tests to add/update: Add loopback IIOP tests for KeyAddr, ProfileAddr, ReferenceAddr, stringified IOR restart, adapter activation, servant-manager dispatch, wrong ORB, stale object, malformed durable key, unregistered path, and Native Image smoke coverage.
Documentation to update: Runtime architecture, POA design, CDR/GIOP/IIOP architecture, CORBA conformance matrix, Native Image matrix if touched, roadmap index, README ready-task status, and this task.
Commands to run: ./gradlew :modules:corba-iiop:test :modules:corba-orb-core:test :modules:corba-poa:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: IIOP preserves durable keys as opaque bytes, ORB/POA owns durable-key parsing and rehydration lookup, all target-address forms route consistently, hostile and stale keys produce deterministic system exceptions, no forbidden runtime mechanisms are introduced, and live peer durable persistence remains unapproved.
Rollback notes: Revert IIOP durable routing code, tests, docs, Native Image updates, and roadmap status changes together.
