# G12-130 Persistent IOR Round Trip

Task ID: G12-130-PERSISTENT-IOR-ROUNDTRIP
Status: blocked
Gate: G12 post-1.0 runtime identity implementation
Requirement IDs: REQ-IOR-001, REQ-IOR-002, REQ-ORB-001, REQ-POA-001, REQ-SEC-006, REQ-NATIVE-002, REQ-INTEROP-009
ADR IDs: ADR-0001, ADR-0005, ADR-0008, ADR-0010, ADR-0014
Specification references: CORBA-IOP-IOR, CORBA-IOP-GIOP, CORBA-IOP-IIOP, CORBA-IF-OBJECT-REF
Target module: modules/corba-ior, modules/corba-iiop, modules/corba-orb-core, modules/corba-poa
Allowed files: modules/corba-ior/src/**, modules/corba-iiop/src/**, modules/corba-orb-core/src/**, modules/corba-poa/src/**, modules/corba-native-image/src/**, docs/architecture/cdr-giop-iiop.md, docs/architecture/runtime-architecture.md, docs/conformance/corba-3.4-matrix.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g12-130-persistent-ior-roundtrip.md, docs/roadmap/tasks/g12-140-naming-persistence-implementation.md, README.md
Forbidden files: peer artifacts, committed live interop reports, optional service implementation, Naming persistence store implementation, Java serialization metadata, reflection metadata, runtime bytecode generation, dynamic proxies, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Blocked until G12-120 completes. Emit, parse, stringify, and route persistent IORs carrying durable object keys through local loopback IIOP.
Tests to add/update: IOR wire/stringified round-trip tests, loopback IIOP dispatch tests after simulated restart, malformed-key hostile-input tests, structured interop report tests, and Native Image smoke coverage.
Documentation to update: CDR/GIOP/IIOP architecture, runtime architecture, CORBA conformance matrix, interop matrix, roadmap index, README ready-task status, this task, and G12-140 status when complete.
Commands to run: ./gradlew :modules:corba-ior:test :modules:corba-iiop:test :modules:corba-orb-core:test :modules:corba-poa:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Persistent IORs preserve durable object keys through binary and stringified forms, route after restart in local loopback IIOP, and reject malformed inputs deterministically; G12-140 is promoted to ready-for-implementation.
Rollback notes: Revert persistent IOR, loopback dispatch, Native Image, interop-report, and documentation updates together.
