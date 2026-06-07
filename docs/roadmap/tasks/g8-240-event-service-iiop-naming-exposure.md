# G8-240 Event Service IIOP Naming Exposure

Task ID: G8-240-EVENT-SERVICE-IIOP-NAMING-EXPOSURE
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-020, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0018
Specification references: EVNT-12
Target module: modules/corba-event-service, modules/corba-iiop, modules/corba-naming-server
Allowed files: modules/corba-event-service/src/**, modules/corba-event-service/build.gradle, modules/corba-event-service/README.md, modules/corba-iiop/src/**, modules/corba-iiop/build.gradle, modules/corba-naming-server/src/**, modules/corba-naming-server/build.gradle, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-240-event-service-iiop-naming-exposure.md, docs/roadmap/tasks/g8-250-event-service-native-smoke.md, README.md
Forbidden files: Native Image smoke entrypoints, interop harness changes, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated OMG compatibility APIs outside explicit descriptors/codecs, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add static descriptors, codecs, dispatchers, network helpers, and local clients for the supported CosEventChannelAdmin/CosEventComm subset over loopback IIOP with optional Naming registration. Supported operations are channel admin lookup, proxy creation, push, pull, try_pull, and disconnect.
Tests to add/update: Add loopback IIOP tests for success paths, Naming bind/resolve, malformed object keys, unknown operations, invalid CDR bodies, stale proxies, bounded diagnostics, and clean shutdown.
Documentation to update: Services design, optional services conformance/review, interop matrix, roadmap index, README, and module README.
Commands to run: ./gradlew :modules:corba-event-service:test :modules:corba-iiop:test :modules:corba-naming-server:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Event Service loopback IIOP/Naming exposure works through explicit descriptors/codecs with no reflection or dynamic dispatch; G8-250 is promoted after completion; no live peer execution is claimed.
Rollback notes: Revert Event Service IIOP/Naming exposure, tests, docs, and roadmap status changes together.
