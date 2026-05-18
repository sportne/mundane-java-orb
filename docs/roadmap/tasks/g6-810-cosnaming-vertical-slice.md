# G6-810 CosNaming Vertical Slice

Task ID: G6-810-COSNAMING-VERTICAL-SLICE
Status: complete
Gate: G6 naming and interop vertical slice
Requirement IDs: REQ-NAM-001, REQ-IOR-002, REQ-ORB-001, REQ-DOC-003
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010
Specification references: NAM-13-SERVICE, NAM-13-COSNAMING, NAM-13-CONTEXT, NAM-13-ITERATOR, NAM-13-STRINGIFIED, NAM-13-URLS
Target module: modules/corba-naming-api, modules/corba-naming-server
Allowed files: modules/corba-naming-api/src/**, modules/corba-naming-api/build.gradle, modules/corba-naming-api/README.md, modules/corba-naming-server/src/**, modules/corba-naming-server/build.gradle, modules/corba-naming-server/README.md, modules/corba-orb-core/src/**, modules/corba-orb-core/README.md, docs/conformance/naming-service-matrix.md, docs/conformance/corba-3.4-matrix.md, docs/architecture/runtime-architecture.md, README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-810-cosnaming-vertical-slice.md, docs/roadmap/tasks/g6-910-native-image-binaries.md
Forbidden files: optional services, external peer artifact downloads, unrelated ORB features
Expected behavior: Task type: implementation. Implement a functional Naming Service client/server slice with bind, rebind, resolve, unbind, list, destroy, and corbaname integration.
Tests to add/update: Unit and local integration tests for naming operations and object URL behavior.
Documentation to update: Naming package docs and naming conformance matrix.
Commands to run: ./gradlew :modules:corba-orb-core:test :modules:corba-naming-api:test :modules:corba-naming-server:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Local JVM client/server naming scenarios pass before external ORB interop is attempted.
Rollback notes: Revert CosNaming implementation, tests, and docs together.
