# G6-160 Minimal IDL-to-Java Generation

Task ID: G6-160-IDL-JAVA-MINIMAL-GENERATION
Status: complete
Gate: G6 IDL compiler vertical slice
Requirement IDs: REQ-IDLJ-002, REQ-IDLJ-003, REQ-IDLJ-004, REQ-DOC-005
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010
Specification references: I2JAV-13-MODULES, I2JAV-13-BASIC, I2JAV-13-INTERFACES, I2JAV-13-PORTABILITY
Target module: modules/corba-idl-java-mapping, modules/corba-codegen
Allowed files: modules/corba-idl-java-mapping/build.gradle, modules/corba-idl-java-mapping/src/main/**, modules/corba-idl-java-mapping/src/test/**, modules/corba-idl-java-mapping/README.md, modules/corba-codegen/build.gradle, modules/corba-codegen/src/main/**, modules/corba-codegen/src/test/**, modules/corba-codegen/README.md, docs/architecture/idl-compiler-architecture.md, docs/conformance/idl-to-java-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-160-idl-java-minimal-generation.md, docs/roadmap/tasks/g6-210-generated-hello-golden-source.md, README.md
Forbidden files: ORB runtime, protocol transport, real peer interop, runtime bytecode generation
Expected behavior: Task type: implementation. Generate deterministic Java source for the minimal IDL subset in legacy and modern modes where approved for the slice.
Tests to add/update: Mapping model tests, golden-source tests, generated-source compilation tests, deterministic ordering tests, and negative dependency-surface checks.
Documentation to update: Mapping package docs, codegen package docs, module READMEs, IDL compiler architecture, IDL-to-Java conformance rows, and roadmap status.
Commands to run: ./gradlew :modules:corba-idl-java-mapping:test :modules:corba-codegen:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Generated source is deterministic, documented, compile-safe in explicit legacy and modern modes, and does not require reflection, `org.omg.*`, ORB runtime APIs, CDR, helpers, holders, stubs, skeletons, POA classes, or dynamic class loading.
Rollback notes: Revert mapping/codegen implementation, tests, and conformance updates together.
