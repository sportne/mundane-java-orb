# G8-370 Notification Service Native Image Smoke

Task ID: G8-370-NOTIFICATION-SERVICE-NATIVE-SMOKE
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-030, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0019
Specification references: NOT-11
Target module: modules/corba-notification-service, modules/corba-native-image
Allowed files: modules/corba-notification-service/src/**, modules/corba-notification-service/README.md, modules/corba-native-image/src/**, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-370-notification-service-native-smoke.md, docs/roadmap/tasks/g8-380-notification-service-interop-metadata.md, README.md
Forbidden files: live peer execution, committed interop reports, peer artifacts, Docker layers, peer manifest metadata, reflection metadata, dynamic proxies, Java serialization metadata, service-loader discovery, runtime bytecode generation, scripting engines, process execution in production sources, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add Native Image smoke coverage for the supported Notification Service subset: channel creation, structured event validation, filter validation, QoS rejection, local delivery, loopback IIOP/Naming exposure, and clean shutdown. Use the repository's native-image discovery conventions and add no metadata files unless explicitly justified by source-level review.
Tests to add/update: Add JVM parity tests for the smoke entrypoint and native smoke wiring; update source-level Native Image metadata audits if needed.
Documentation to update: Notification README, services design, optional services conformance/review, Native Image matrix, roadmap index, README, and G8-380 status.
Commands to run: ./gradlew :modules:corba-notification-service:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Notification Service has representative JVM parity and Native Image smoke coverage with no reflection/dynamic-proxy/serialization metadata or live peer claim; G8-380 is promoted after completion.
Rollback notes: Revert Native Image smoke sources, tests, docs, and roadmap status together.
