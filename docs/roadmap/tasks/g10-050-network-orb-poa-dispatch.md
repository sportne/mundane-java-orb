# G10-050 Network ORB POA Dispatch

Task ID: G10-050-NETWORK-ORB-POA-DISPATCH
Status: ready-for-implementation
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-ORB-001, REQ-POA-001, REQ-POA-002, REQ-IIOP-001, REQ-IOR-001, REQ-INTEROP-001, REQ-INTEROP-003, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0007, ADR-0008, ADR-0010
Specification references: CORBA-IF-ORB, CORBA-IF-OBJECT, CORBA-IF-POA, CORBA-IOP-GIOP, CORBA-IOP-IIOP, CORBA-IOP-IOR
Target module: modules/corba-orb-core, modules/corba-poa, modules/corba-iiop, modules/corba-ior, modules/corba-codegen
Allowed files: modules/corba-orb-core/src/**, modules/corba-poa/src/**, modules/corba-iiop/src/**, modules/corba-ior/src/**, modules/corba-codegen/src/**, docs/architecture/runtime-architecture.md, docs/architecture/poa-design.md, docs/conformance/corba-3.4-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-050-network-orb-poa-dispatch.md, docs/roadmap/tasks/g10-060-network-naming-and-urls.md, docs/roadmap/tasks/g10-070-dynamic-any-dii-dsi-and-ir-wire-closure.md, docs/roadmap/tasks/g10-080-portable-interceptors.md, README.md
Forbidden files: peer artifacts, live interop reports, optional service implementation, CORBA Security Service behavior
Expected behavior: Task type: implementation. Bridge IIOP request handling to ORB/POA dispatch for generated stubs and skeletons, including network object references, transient and required persistent POA reference behavior, servant dispatch, exception reply mapping, request IDs, connection lifecycle, and bounded backpressure.
Tests to add/update: Unit, generated-binding, local integration, loopback IIOP, POA policy, servant lifecycle, exception mapping, Native Image smoke, and hostile-input tests.
Documentation to update: Runtime architecture, POA design, conformance rows, Native Image matrix, roadmap index, README ready-task status, this task, and G10-060/G10-070/G10-080 status when complete.
Commands to run: ./gradlew :modules:corba-orb-core:test :modules:corba-poa:test :modules:corba-iiop:test :modules:corba-ior:test :modules:corba-codegen:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Generated server/client paths can dispatch over loopback IIOP through ORB/POA with deterministic request, reply, and failure behavior; security-service policy remains deferred.
Rollback notes: Revert ORB/POA/IIOP/codegen, test, and documentation changes together.
