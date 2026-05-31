# G13-060 POA Durable Path Registry

Task ID: G13-060-POA-DURABLE-PATH-REGISTRY
Status: complete
Gate: G13 durable runtime hardening
Requirement IDs: REQ-ORB-001, REQ-POA-001, REQ-POA-002, REQ-IOR-001, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0005, ADR-0008, ADR-0010, ADR-0014, ADR-0015
Specification references: CORBA-IF-ORB, CORBA-IF-POA, CORBA-IF-OBJECT-REF
Target module: modules/corba-orb-core, modules/corba-poa, modules/corba-native-image
Allowed files: modules/corba-orb-core/src/**, modules/corba-poa/src/**, modules/corba-native-image/src/**, docs/architecture/runtime-architecture.md, docs/architecture/poa-design.md, docs/conformance/corba-3.4-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g13-060-poa-durable-path-registry.md, docs/roadmap/tasks/g13-070-poa-adapter-activation-lookup.md, README.md
Forbidden files: peer artifacts, committed live interop reports, interop cache files, optional service implementation, Java serialization metadata, reflection metadata, runtime bytecode generation, dynamic proxies, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Maintainer approval after G13-050 unblocked this task. Add an explicit durable POA path registry for caller-approved persistent POA paths without adding adapter activation or servant rehydration behavior.
Tests to add/update: Add unit tests for registry registration, duplicate path rejection, path traversal rejection, wrong-ORB rejection, bounds enforcement, shutdown behavior, and Native Image smoke coverage for the public registry entrypoints.
Documentation to update: Runtime architecture, POA design, CORBA conformance matrix, Native Image matrix if touched, roadmap index, README ready-task status, this task, and G13-070 status when complete.
Commands to run: ./gradlew :modules:corba-orb-core:test :modules:corba-poa:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Durable POA paths can be registered explicitly under durable ORB identity, invalid paths and wrong namespaces fail deterministically before activation side effects, transient ORBs cannot register durable paths, no reflection or serialization mechanisms are introduced, and G13-070 is promoted for implementation.
Rollback notes: Revert durable path registry code, tests, docs, Native Image updates, and roadmap status changes together.
