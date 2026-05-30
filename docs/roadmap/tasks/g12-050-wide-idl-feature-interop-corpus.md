# G12-050 Wide IDL Feature Interop Corpus

Task ID: G12-050-WIDE-IDL-FEATURE-INTEROP-CORPUS
Status: blocked
Gate: G12 post-1.0 compiler and interop hardening
Requirement IDs: REQ-IDL-001, REQ-IDL-002, REQ-IDLJ-002, REQ-IDLJ-004, REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-009, REQ-NATIVE-005
ADR IDs: ADR-0001, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010
Specification references: IDL-42-GRAMMAR, IDL-42-SCOPING, I2JAV-13, CORBA-IOP-CDR, CORBA-IOP-GIOP, CORBA-IOP-IIOP
Target module: modules/corba-interop-testkit, interop
Allowed files: modules/corba-interop-testkit/src/**, modules/corba-native-image/src/**, interop/idl/**, interop/bin/**, interop/README.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/conformance/idl-4.2-matrix.md, docs/conformance/idl-to-java-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g12-050-wide-idl-feature-interop-corpus.md, docs/roadmap/tasks/g12-060-peer-idl-feature-interop-matrix.md, README.md
Forbidden files: peer artifacts, interop cache files, live interop reports, optional service implementation, production ORB/POA/IIOP behavior outside narrow fixture adapters explicitly allowed by a later amended task contract
Expected behavior: Task type: verification-only. Add a project-owned broad IDL feature corpus and deterministic local interop scenarios that exercise richer IDL constructs across parser, semantics, mapping, generated source compilation, local JVM lanes, Native Image lanes, and structured interop reports without claiming new live peer compatibility yet.
Tests to add/update: Interop testkit tests for corpus discovery and report schema, generated-source compilation tests for each corpus fixture, Native Image smoke tests for selected client/server fixture paths, and structured-report tests for unsupported or missing-prerequisite outcomes.
Documentation to update: Interop matrix, Native Image matrix, IDL and IDL-to-Java conformance matrices, roadmap index, README ready-task status, this task, and G12-060 status when complete.
Commands to run: ./gradlew :modules:corba-interop-testkit:test :modules:corba-native-image:test :modules:corba-codegen:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: The broad IDL feature corpus is tracked as source fixtures, local JVM and Native Image lanes produce deterministic structured reports, unsupported live-peer directions are classified without raw live outputs, and G12-060 is promoted to ready-for-implementation.
Rollback notes: Revert interop fixture, native-smoke, report-schema, and documentation updates together.
