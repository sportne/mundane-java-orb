# G10-020 IDL To Java Legacy Mapping Closure

Task ID: G10-020-IDL-TO-JAVA-LEGACY-MAPPING-CLOSURE
Status: complete
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-IDLJ-002, REQ-IDLJ-004, REQ-NATIVE-002, REQ-INTEROP-009
ADR IDs: ADR-0001, ADR-0005, ADR-0007, ADR-0008, ADR-0010
Specification references: I2JAV-13, IDL-42-GRAMMAR, CORBA-IF-TYPECODE
Target module: modules/corba-idl-java-mapping, modules/corba-codegen, modules/corba-idlj-cli
Allowed files: modules/corba-idl-java-mapping/src/**, modules/corba-codegen/src/**, modules/corba-idlj-cli/src/**, modules/corba-testkit/src/**, docs/architecture/idl-compiler-architecture.md, docs/conformance/idl-to-java-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-020-idl-to-java-legacy-mapping-closure.md, docs/roadmap/tasks/g10-030-omg-api-compatibility-surface.md, README.md
Forbidden files: peer artifacts, live interop reports, optional service implementation, unrelated ORB transport behavior
Expected behavior: Task type: implementation. Generate legacy-compatible helpers, holders, stubs, POA skeletons, user-exception helpers, sequence/array/union mappings, object-reference mappings, inherited interfaces, and static descriptor/codec metadata for the G10 IDL subset.
Tests to add/update: Golden-source, compile-test, mapper, codegen, CLI, downstream sample, Native Image source-policy, and hostile-input tests for the expanded generated surface.
Documentation to update: IDL compiler architecture, IDL-to-Java conformance matrix, Native Image matrix, roadmap index, README ready-task status, this task, and G10-030 status when complete.
Commands to run: ./gradlew :modules:corba-idl-java-mapping:test :modules:corba-codegen:test :modules:corba-idlj-cli:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Generated Java for the G10 IDL subset compiles deterministically, avoids runtime classpath scanning/reflection metadata, and produces descriptor/codec surfaces usable by later ORB and interop tasks.
Rollback notes: Revert mapping, generated-source, fixture, and documentation changes together.
