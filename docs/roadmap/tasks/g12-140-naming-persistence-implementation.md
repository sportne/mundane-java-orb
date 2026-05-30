# G12-140 Naming Persistence Implementation

Task ID: G12-140-NAMING-PERSISTENCE-IMPLEMENTATION
Status: blocked
Gate: G12 post-1.0 runtime identity implementation
Requirement IDs: REQ-NAM-001, REQ-IOR-002, REQ-ORB-001, REQ-SEC-006, REQ-NATIVE-002, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0005, ADR-0008, ADR-0010, ADR-0014
Specification references: NAM-13-SERVICE, NAM-13-COSNAMING, NAM-13-URLS, CORBA-IOP-IOR
Target module: modules/corba-naming-server, modules/corba-naming-api, modules/corba-ior
Allowed files: modules/corba-naming-server/src/**, modules/corba-naming-api/src/**, modules/corba-ior/src/**, modules/corba-native-image/src/**, docs/architecture/services-design.md, docs/architecture/runtime-architecture.md, docs/conformance/naming-service-matrix.md, docs/conformance/corba-3.4-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g12-140-naming-persistence-implementation.md, README.md
Forbidden files: optional service implementation, peer artifacts, committed live interop reports, Java serialization metadata, reflection metadata, runtime bytecode generation, dynamic proxies, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Blocked until G12-130 completes. Add caller-configured Naming persistence for durable object-reference bindings and naming contexts using a bounded versioned store format.
Tests to add/update: Naming restart simulation tests, store corruption and path traversal tests, bounded decode tests, persistent corbaname resolution tests, and Native Image smoke coverage for configured persistence.
Documentation to update: Services design, runtime architecture, Naming and CORBA conformance matrices, Native Image matrix, roadmap index, README ready-task status, and this task.
Commands to run: ./gradlew :modules:corba-naming-server:test :modules:corba-naming-api:test :modules:corba-ior:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Naming persistence stores only durable IOR/context data, survives restart with deterministic resolution, rejects malformed stores safely, and remains Native Image friendly without reflection or serialization metadata.
Rollback notes: Revert Naming persistence runtime, store codecs, Native Image, and documentation updates together.
