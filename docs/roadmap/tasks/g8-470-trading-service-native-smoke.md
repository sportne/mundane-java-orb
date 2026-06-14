# G8-470 Trading Service Native Image Smoke

Task ID: G8-470-TRADING-SERVICE-NATIVE-SMOKE
Status: complete
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-010, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0020
Specification references: TRADE-10
Target module: modules/corba-trading-service, modules/corba-native-image
Allowed files: modules/corba-trading-service/src/**, modules/corba-trading-service/README.md, modules/corba-native-image/src/**, modules/corba-native-image/build.gradle, modules/corba-native-image/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-470-trading-service-native-smoke.md, docs/roadmap/tasks/g8-480-trading-service-interop-metadata.md, README.md
Forbidden files: Native Image reflection metadata, dynamic proxy metadata, Java serialization metadata, classpath scanning, service-loader discovery, runtime bytecode generation, scripting engines, process execution in production sources, live peer execution, committed native binaries, committed live interop reports, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add Trading Service Native Image smoke coverage for type registration, offer registration, constraint rejection, local query, loopback IIOP/Naming exposure, import/export disabled diagnostics, and clean shutdown. Extend source-level Native Image audits to Trading Service production sources. Do not add metadata files or live peer behavior.
Tests to add/update: Add JVM parity tests for the smoke entrypoint, optional native-image smoke wiring following existing discovery policy, source-level audit coverage for Trading Service production sources, and package documentation checks as needed.
Documentation to update: Trading Service README, Native Image README/matrix, services design, optional services conformance/review, roadmap index, README, and G8-480 status.
Commands to run: ./gradlew :modules:corba-trading-service:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Trading Service has closed-world friendly JVM parity and optional Native Image smoke coverage with no reflection, dynamic proxy, serialization, service-loader, bytecode-generation metadata, native binary commits, interop, or live peer claim; G8-480 is promoted after completion.
Completion evidence: Added the `TradingServiceNativeSmoke` entrypoint and `tradingService` aggregate Native Image smoke target. The smoke exercises type registration, offer registration, bounded constraint rejection, local query, import/export disabled diagnostics, descriptor-backed loopback IIOP query and withdrawal, import metadata listing, Naming-resolved Trader IORs, and clean shutdown. JVM parity tests invoke the Trading smoke before native compilation, and the source-level Native Image audit now includes Trading Service production sources. No Native Image metadata files, dynamic proxies, Java serialization metadata, service-loader discovery, runtime bytecode generation, committed native binaries, interop metadata, remote federation execution, or live peer claims are added. G8-480 is promoted as the only ready Trading Service implementation task.
Rollback notes: Revert Trading Service Native Image smoke sources, tests, docs, and roadmap status together.
