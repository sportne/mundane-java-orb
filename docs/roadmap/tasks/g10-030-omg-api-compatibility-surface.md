# G10-030 OMG API Compatibility Surface

Task ID: G10-030-OMG-API-COMPATIBILITY-SURFACE
Status: ready-for-implementation
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-ORB-001, REQ-POA-001, REQ-DYN-001, REQ-NAM-001, REQ-IDLJ-002, REQ-INTEROP-009
ADR IDs: ADR-0001, ADR-0003, ADR-0005, ADR-0007, ADR-0008
Specification references: CORBA-IF, CORBA-IF-ORB, CORBA-IF-POA, CORBA-IF-DYNANY, NAM-13, I2JAV-13
Target module: modules/corba-omg-api
Allowed files: modules/corba-omg-api/src/**, modules/corba-architecture-tests/src/**, docs/architecture/module-boundaries.md, docs/conformance/corba-2.3-legacy-java-matrix.md, docs/conformance/corba-3.4-matrix.md, docs/conformance/naming-service-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-030-omg-api-compatibility-surface.md, docs/roadmap/tasks/g10-040-cdr-giop-ior-wire-closure.md, README.md
Forbidden files: runtime implementation outside compatibility API surfaces, generated artifacts, peer artifacts, optional service APIs beyond existing human-gated service placeholders
Expected behavior: Task type: implementation. Add the minimal source-compatible `org.omg.*` and `CosNaming` API surfaces required by generated bindings and non-optional peer scenarios, including ORB, POA, DynamicAny, DII/DSI-facing, Portable Interceptor-facing, and Naming types.
Tests to add/update: API construction, exception/value contracts, source compatibility compile tests, architecture boundary tests, and Native Image source-policy tests.
Documentation to update: Module boundaries, conformance matrices, roadmap index, README ready-task status, this task, and G10-040 status when complete.
Commands to run: ./gradlew :modules:corba-omg-api:test :modules:corba-architecture-tests:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Generated code and peer fixture code compile against the compatibility APIs without moving runtime ownership into `corba-omg-api`; optional services stay deferred.
Rollback notes: Revert API, architecture test, and documentation changes together.
