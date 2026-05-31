# G13-020 Naming Store Operational Hardening

Task ID: G13-020-NAMING-STORE-OPERATIONAL-HARDENING
Status: complete
Gate: G13 durable runtime hardening
Requirement IDs: REQ-NAM-001, REQ-IOR-002, REQ-ORB-001, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0005, ADR-0008, ADR-0010, ADR-0014
Specification references: NAM-13-SERVICE, NAM-13-COSNAMING, NAM-13-URLS, CORBA-IOP-IOR
Target module: modules/corba-naming-server, modules/corba-native-image
Allowed files: modules/corba-naming-server/src/**, modules/corba-native-image/src/**, docs/architecture/services-design.md, docs/conformance/naming-service-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g13-020-naming-store-operational-hardening.md, docs/roadmap/tasks/g13-030-mjns-store-versioning-policy.md, README.md
Forbidden files: peer artifacts, committed live interop reports, interop cache files, optional service implementation, Java serialization metadata, reflection metadata, runtime bytecode generation, dynamic proxies, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Blocked until G13-010 completes. Harden the caller-configured Naming persistence store with narrow operational write hygiene while preserving the existing single-file `MJNS` v1 store model.
Tests to add/update: Add Naming persistence tests for directory store-path rejection, failed-write cleanup where the filesystem permits it, durable temp-file write behavior, fallback non-atomic replacement behavior, and Native Image smoke coverage if the persistence entrypoint changes.
Documentation to update: Services design, Naming conformance matrix, Native Image matrix if touched, roadmap index, README ready-task status, this task, and G13-030 status when complete.
Commands to run: ./gradlew :modules:corba-naming-server:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: The store rejects directories deterministically, forces temp file contents before replace when supported by the JDK/filesystem, attempts best-effort temp cleanup after write failures, keeps backup/retention/store-permission management as caller/operator responsibility, and promotes G13-030 to ready-for-implementation.
Rollback notes: Revert Naming persistence write-path changes, tests, Native Image updates, documentation updates, and G13 status promotion together.
