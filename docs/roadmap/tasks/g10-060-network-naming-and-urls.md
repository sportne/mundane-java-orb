# G10-060 Network Naming And URLs

Task ID: G10-060-NETWORK-NAMING-AND-URLS
Status: blocked
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-NAM-001, REQ-IOR-002, REQ-ORB-001, REQ-IIOP-001, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0007, ADR-0008, ADR-0010
Specification references: NAM-13-SERVICE, NAM-13-COSNAMING, NAM-13-CONTEXT, NAM-13-ITERATOR, NAM-13-STRINGIFIED, NAM-13-URLS, CORBA-IOP-OBJECT-URL
Target module: modules/corba-naming-api, modules/corba-naming-server, modules/corba-ior, modules/corba-orb-core, modules/corba-iiop
Allowed files: modules/corba-naming-api/src/**, modules/corba-naming-server/src/**, modules/corba-ior/src/**, modules/corba-orb-core/src/**, modules/corba-iiop/src/**, docs/conformance/naming-service-matrix.md, docs/conformance/corba-3.4-matrix.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-060-network-naming-and-urls.md, docs/roadmap/tasks/g10-100-native-image-interop-binaries.md, README.md
Forbidden files: optional service implementation, peer artifacts, live interop reports, unrelated ORB policy behavior
Expected behavior: Task type: implementation. Add peer-visible Naming Service behavior over IIOP, remote `corbaloc` and `corbaname` resolution, IOR exchange, stringified names, bind/rebind/resolve/unbind/list/destroy, and deterministic server fixture setup.
Tests to add/update: Naming API/server unit tests, networked naming loopback tests, URL parsing/resolution tests, generated fixture tests, missing-name diagnostics, Native Image smoke, and structured interop dry-run tests.
Documentation to update: Naming conformance matrix, CORBA conformance matrix, interop matrix, roadmap index, README ready-task status, this task, and G10-100 status when complete.
Commands to run: ./gradlew :modules:corba-naming-api:test :modules:corba-naming-server:test :modules:corba-ior:test :modules:corba-orb-core:test :modules:corba-iiop:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: This task remains blocked until G10-050 is complete; Naming scenarios can run over network IIOP locally and provide the behavior needed by full peer interop execution.
Rollback notes: Revert naming, URL, ORB/IIOP integration, test, and documentation changes together.
