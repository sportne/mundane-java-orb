# G8-480 Trading Service Interop Metadata

Task ID: G8-480-TRADING-SERVICE-INTEROP-METADATA
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-010, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0020
Specification references: TRADE-10
Target module: interop harness, modules/corba-interop-testkit, modules/corba-trading-service
Allowed files: interop/bin/**, interop/idl/**, interop/lib/**, interop/peers/**/peer.yaml, modules/corba-interop-testkit/src/**, modules/corba-trading-service/README.md, docs/architecture/services-design.md, docs/conformance/optional-services-matrix.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-480-trading-service-interop-metadata.md, docs/roadmap/tasks/g8-490-trading-service-conformance-closure.md, README.md
Forbidden files: live peer execution approval, committed live reports, peer artifacts, Docker layers, prepared peer images, generated OMG APIs, broad peer compatibility claims, durable persistence, remote federation execution, Native Image binary commits, Java serialization metadata, reflection metadata, scripting engines, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Add Trading Service interop metadata and reporting only: a `trading-service` IDL fixture for the implemented TRADE-10 subset, approved-peer manifest scenario declarations, `InteropScenario.tradingService()`, dry-run direction enumeration for JVM/native local runtimes, and structured missing-prerequisite reports. `--require-live` must never start peer containers or local live lanes for Trading Service; it only writes deterministic prerequisite reports.
Tests to add/update: Add interop testkit and harness tests for fixture presence, stable scenario identity, all approved peer manifests declaring `trading-service`, manifest validation requiring capability metadata, `all` target filtering, dry-run matrix enumeration with no report/log/IOR path creation, missing live approval, scenario IDL, local JVM command, native binary, artifact cache, digest-pinned base image, container runtime, and peer image reports, report fields, and ignored raw evidence paths under build output.
Documentation to update: Trading Service README, services design, optional services conformance/review, interop matrix, roadmap index, README, and G8-490 status.
Commands to run: ./gradlew :modules:corba-interop-testkit:test :modules:corba-trading-service:test; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer run-direction-matrix --dry-run trading-service all; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Trading Service is discoverable as metadata-only interop scenario with deterministic dry-run and missing-prerequisite reports and no live peer execution, prepared peer artifacts, committed reports, or compatibility claim; G8-490 is promoted after completion.
Rollback notes: Revert Trading Service interop metadata, tests, docs, peer manifest declarations, and roadmap status together.
