# G13-030 MJNS Store Versioning Policy

Task ID: G13-030-MJNS-STORE-VERSIONING-POLICY
Status: ready-for-implementation
Gate: G13 durable runtime hardening
Requirement IDs: REQ-NAM-001, REQ-IOR-002, REQ-ORB-001, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0005, ADR-0008, ADR-0010, ADR-0014
Specification references: NAM-13-SERVICE, NAM-13-COSNAMING, CORBA-IOP-IOR
Target module: modules/corba-naming-server
Allowed files: modules/corba-naming-server/src/**, docs/architecture/services-design.md, docs/conformance/naming-service-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g13-030-mjns-store-versioning-policy.md, README.md
Forbidden files: store migration implementation, alternate persistence engines, peer artifacts, committed live interop reports, optional service implementation, Java serialization metadata, reflection metadata, runtime bytecode generation, dynamic proxies, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Blocked until G13-020 completes. Record and test the `MJNS` version 1 compatibility policy without adding migrations or a second store format.
Tests to add/update: Add explicit store codec tests for the v1 binary layout, unsupported versions, trailing bytes, malformed UTF-8, oversized string/store/context/binding records, wrong ORB id, wrong Naming context repository id, wrong Naming context key namespace, malformed durable keys, and transient target IORs.
Documentation to update: Services design, Naming conformance matrix, roadmap index, README ready-task status, this task, and G13-040 status when complete.
Commands to run: ./gradlew :modules:corba-naming-server:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: The documented `MJNS` v1 layout matches the implementation, unsupported future versions fail deterministically, hostile record cases are directly covered by tests, migrations remain explicitly deferred to a later task, and G13-040 is promoted to ready-for-implementation.
Rollback notes: Revert store-version documentation, tests, and any codec-only validation changes together.
