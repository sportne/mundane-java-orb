# G6-210 Generated Hello Golden Source

Task ID: G6-210-GENERATED-HELLO-GOLDEN-SOURCE
Status: complete
Gate: G6 generated-code vertical slice
Requirement IDs: REQ-IDLJ-001, REQ-IDLJ-002, REQ-IDLJ-003, REQ-DOC-005, REQ-NFR-007
ADR IDs: ADR-0003, ADR-0005, ADR-0010
Specification references: I2JAV-13-MODULES, I2JAV-13-INTERFACES, IDL-42-GRAMMAR
Target module: modules/corba-codegen, modules/corba-idlj-cli, modules/corba-testkit
Allowed files: modules/corba-codegen/src/**, modules/corba-codegen/README.md, modules/corba-idlj-cli/src/**, interop/idl/**, docs/verification/fixture-conventions.md, docs/conformance/idl-to-java-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-210-generated-hello-golden-source.md, docs/roadmap/tasks/g6-220-generated-codecs-descriptors.md, README.md
Forbidden files: ORB network runtime, GIOP/IIOP transport, peer artifacts
Expected behavior: Task type: implementation. Compile a tiny Hello IDL module to Java through the test-only parser, semantics, mapper, and codegen pipeline, compare golden output, and compile generated source as a test artifact without adding public CLI generation.
Tests to add/update: Resource-backed golden-source tests, generated-code compilation tests, forbidden generated-token checks, and CLI validation of the shared Hello IDL fixture.
Documentation to update: Generated-code verification notes, fixture conventions, codegen README, roadmap status, and conformance test IDs.
Commands to run: ./gradlew :modules:corba-codegen:test :modules:corba-idlj-cli:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: The first end-to-end IDL-to-source vertical slice is deterministic, locally testable, and remains generation-CLI free.
Rollback notes: Revert generated Hello fixtures, codegen changes, tests, and docs together.
