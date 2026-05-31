# G13-070 POA Adapter Activation Lookup

Task ID: G13-070-POA-ADAPTER-ACTIVATION-LOOKUP
Status: ready-for-implementation
Gate: G13 durable runtime hardening
Requirement IDs: REQ-ORB-001, REQ-POA-001, REQ-POA-002, REQ-IOR-001, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0005, ADR-0008, ADR-0010, ADR-0014, ADR-0015
Specification references: CORBA-IF-ORB, CORBA-IF-POA, CORBA-IF-OBJECT-REF
Target module: modules/corba-poa, modules/corba-orb-core, modules/corba-native-image
Allowed files: modules/corba-poa/src/**, modules/corba-orb-core/src/**, modules/corba-native-image/src/**, docs/architecture/runtime-architecture.md, docs/architecture/poa-design.md, docs/conformance/corba-3.4-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g13-070-poa-adapter-activation-lookup.md, docs/roadmap/tasks/g13-080-poa-servant-manager-rehydration.md, README.md
Forbidden files: peer artifacts, committed live interop reports, interop cache files, optional service implementation, Java serialization metadata, reflection metadata, runtime bytecode generation, dynamic proxies, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Route valid durable POA paths through explicit adapter activation lookup when the addressed persistent POA is not active.
Tests to add/update: Add tests for active POA lookup, registered adapter activation, unregistered path diagnostics, inactive POA manager behavior, activation failure mapping, hostile key ordering, and Native Image smoke coverage for registered activation factories.
Documentation to update: Runtime architecture, POA design, CORBA conformance matrix, Native Image matrix if touched, roadmap index, README ready-task status, this task, and G13-080 status when complete.
Commands to run: ./gradlew :modules:corba-poa:test :modules:corba-orb-core:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: A valid durable key can locate an active persistent POA or activate an approved path through explicit caller registration, malformed and wrong-namespace keys never trigger activation, failures map to deterministic CORBA system exceptions, and G13-080 is promoted only if maintainers approve continuing the implementation sequence.
Rollback notes: Revert adapter activation lookup code, tests, docs, Native Image updates, and roadmap status changes together.
