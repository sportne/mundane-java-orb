# G10-080 Portable Interceptors

Task ID: G10-080-PORTABLE-INTERCEPTORS
Status: complete
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-ORB-001, REQ-IIOP-001, REQ-SEC-003, REQ-NATIVE-002, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0007, ADR-0008, ADR-0010
Specification references: CORBA-IF-PI, CORBA-IOP-SERVICE-CONTEXT, CORBA-IOP-GIOP
Target module: modules/corba-interceptors, modules/corba-orb-core, modules/corba-giop, modules/corba-iiop, modules/corba-omg-api
Allowed files: modules/corba-interceptors/src/**, modules/corba-interceptors/build.gradle, modules/corba-interceptors/README.md, modules/corba-orb-core/src/**, modules/corba-giop/src/**, modules/corba-iiop/src/**, modules/corba-iiop/build.gradle, modules/corba-omg-api/src/**, docs/architecture/runtime-architecture.md, docs/conformance/corba-3.4-matrix.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-080-portable-interceptors.md, docs/roadmap/tasks/g10-090-rmi-iiop-compatibility-closure.md, docs/roadmap/tasks/g10-100-native-image-interop-binaries.md, README.md
Forbidden files: CORBA Security Service behavior, optional service implementation, peer artifacts, live interop reports
Expected behavior: Task type: implementation. Implemented non-optional Portable Interceptor request-flow behavior needed by peer scenarios: registration, client/server interception, service context propagation, ordering, deterministic exceptions, and Native Image-compatible metadata.
Tests to add/update: Interceptor API, ORB/IIOP integration, service-context propagation, ordering, failure, hostile-input, Native Image, and architecture boundary tests.
Documentation to update: Runtime architecture, CORBA conformance rows, interop matrix, Native Image matrix, roadmap index, README ready-task status, this task, and G10-100 status when complete.
Commands to run: ./gradlew :modules:corba-interceptors:test :modules:corba-orb-core:test :modules:corba-giop:test :modules:corba-iiop:test :modules:corba-omg-api:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Interceptor scenarios propagate service contexts deterministically over the ORB/IIOP path without introducing Security Service policy behavior.
Rollback notes: Revert interceptor, ORB/IIOP integration, API, test, and documentation changes together.
