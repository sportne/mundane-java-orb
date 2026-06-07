# G8-250 Event Service Native Image Smoke

Task ID: G8-250-EVENT-SERVICE-NATIVE-SMOKE
Status: ready-for-implementation
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-020, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0018
Specification references: EVNT-12
Target module: modules/corba-event-service, modules/corba-native-image
Allowed files: modules/corba-event-service/src/**, modules/corba-event-service/README.md, modules/corba-native-image/src/**, modules/corba-native-image/build.gradle, docs/verification/native-image-matrix.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-250-event-service-native-smoke.md, docs/roadmap/tasks/g8-260-event-service-interop-metadata.md, README.md
Forbidden files: interop harness changes, live peer execution, committed live interop reports, peer artifacts, Docker layers, generated OMG compatibility APIs, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add Native Image smoke coverage for Event Service channel creation, push delivery, pull delivery, bounded rejection, loopback IIOP/Naming exposure, and clean shutdown.
Tests to add/update: Add Native Image smoke entrypoint tests and update native smoke binaries for Event Service.
Documentation to update: Native Image matrix, services design, optional services conformance/review, roadmap index, README, and module README.
Commands to run: ./gradlew :modules:corba-event-service:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check; SDKMAN GraalVM ./gradlew :modules:corba-native-image:nativeImageBinariesSmoke
Acceptance criteria: Event Service is covered by JVM tests and SDKMAN GraalVM Native Image smoke; G8-260 is promoted after completion; no peer execution is performed.
Rollback notes: Revert Event Service Native Image smoke code, tests, docs, and roadmap status changes together.
