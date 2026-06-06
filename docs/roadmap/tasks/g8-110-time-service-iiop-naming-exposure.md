# G8-110 Time Service IIOP Naming Exposure

Task ID: G8-110-TIME-SERVICE-IIOP-NAMING-EXPOSURE
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-060, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0017
Specification references: TIME-11
Target module: modules/corba-time-service, modules/corba-iiop, modules/corba-naming-server, modules/corba-native-image
Allowed files: modules/corba-time-service/src/**, modules/corba-time-service/build.gradle, modules/corba-time-service/README.md, modules/corba-iiop/src/**, modules/corba-iiop/build.gradle, modules/corba-naming-server/src/**, modules/corba-naming-server/build.gradle, modules/corba-native-image/src/**, modules/corba-native-image/build.gradle, modules/corba-interop-testkit/src/**, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-110-time-service-iiop-naming-exposure.md, docs/roadmap/tasks/g8-120-time-service-interop-metadata.md, README.md
Forbidden files: unrelated service implementation, live peer execution, committed live interop reports, peer artifacts, Docker layers, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Expose the local Time Service through explicit static IIOP operation descriptors and optional Naming registration. Keep object keys caller-configured or deterministic test fixtures; do not add live peer claims.
Tests to add/update: Add loopback IIOP tests for universal_time, new_universal_time, new_interval, malformed object keys, unknown operations, bounded invalid input diagnostics, and Naming bind/resolve where exposed; update Native Image smoke for exposed entrypoints.
Documentation to update: Services design, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and module README.
Commands to run: ./gradlew :modules:corba-time-service:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: JVM loopback IIOP/Naming Time Service exposure works with explicit descriptors and no reflection or dynamic dispatch; G8-120 is promoted after completion; no live peer execution is claimed.
Rollback notes: Revert Time Service IIOP/Naming exposure, tests, docs, and roadmap status changes together.
