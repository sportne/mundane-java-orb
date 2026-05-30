# G12-110 Durable ORB Identity Foundation

Task ID: G12-110-DURABLE-ORB-IDENTITY-FOUNDATION
Status: ready-for-implementation
Gate: G12 post-1.0 runtime identity implementation
Requirement IDs: REQ-ORB-001, REQ-IOR-001, REQ-SEC-006, REQ-NATIVE-002, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0005, ADR-0008, ADR-0010, ADR-0014
Specification references: CORBA-IF-ORB, CORBA-IF-OBJECT-REF, CORBA-IOP-IOR
Target module: modules/corba-orb-core, modules/corba-ior
Allowed files: modules/corba-orb-core/src/**, modules/corba-ior/src/**, modules/corba-native-image/src/**, docs/architecture/runtime-architecture.md, docs/architecture/cdr-giop-iiop.md, docs/conformance/corba-3.4-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g12-110-durable-orb-identity-foundation.md, docs/roadmap/tasks/g12-120-persistent-poa-object-keys.md, README.md
Forbidden files: peer artifacts, live interop reports, optional service implementation, Java serialization metadata, reflection metadata, runtime bytecode generation, dynamic proxies, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add explicit configured ORB identity values and bounded durable object-key value parsing/formatting primitives without enabling persistent POA dispatch yet.
Tests to add/update: Unit tests for configured ORB ids, invalid ids, durable key encode/decode bounds, transient/durable key separation, and Native Image smoke coverage for the key codec.
Documentation to update: Runtime architecture, CDR/GIOP/IIOP architecture, CORBA conformance matrix, roadmap index, README ready-task status, this task, and G12-120 status when complete.
Commands to run: ./gradlew :modules:corba-orb-core:test :modules:corba-ior:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Durable ORB identity and object-key value codecs are explicit, bounded, Native Image friendly, and covered by tests; persistent POA behavior remains deferred; G12-120 is promoted to ready-for-implementation.
Rollback notes: Revert ORB identity, object-key codec, Native Image, and documentation updates together.
