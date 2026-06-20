# G8-570 Transaction Service Native Image Smoke

Task ID: G8-570-TRANSACTION-SERVICE-NATIVE-SMOKE
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-040, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0021
Specification references: TRANS-14
Target module: modules/corba-transaction-service, modules/corba-native-image
Allowed files: modules/corba-transaction-service/src/**, modules/corba-transaction-service/README.md, modules/corba-native-image/src/**, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-570-transaction-service-native-smoke.md, docs/roadmap/tasks/g8-580-transaction-service-interop-metadata.md, README.md
Forbidden files: interop metadata, live peer execution, committed live interop reports, durable recovery logs, XA integration, Security Service integration, peer distributed two-phase commit claims, Native Image binary commits, reflection metadata, dynamic proxy metadata, Java serialization metadata, service-loader discovery, scripting engines, runtime bytecode generation, process execution in production sources, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add Native Image smoke coverage for coordinator/resource creation, timeout rejection, local rollback and commit paths, propagation metadata validation, request-context boundary behavior, disabled recovery diagnostics, and clean shutdown. Extend source-level audits to Transaction Service production sources without reflection metadata, dynamic proxies, Java serialization metadata, service-loader discovery, scripting engines, bytecode generation, `Unsafe`, `sun.*`, or `jdk.internal.*`. Do not add interop metadata, durable recovery logs, XA, Security Service, distributed peer two-phase commit, or live peer claims.
Tests to add/update: Add JVM parity tests for the smoke entrypoint, extend Native Image target registration and boundary audits, and cover hostile timeout/provenance inputs under the smoke entrypoint.
Documentation to update: Transaction Service README, services design, optional services conformance/review, Native Image matrix, roadmap index, README, and G8-580 status.
Commands to run: ./gradlew :modules:corba-transaction-service:test :modules:corba-native-image:test; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Transaction Service has Native Image smoke coverage and source-level closed-world audits for the implemented local/IIOP subset without metadata files, durable logs, interop, distributed peer two-phase commit, or live peer claims; G8-580 is promoted after completion.
Rollback notes: Revert Transaction Service Native Image smoke sources, tests, docs, and roadmap status together.
