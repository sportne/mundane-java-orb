# G10-090 RMI-IIOP Compatibility Closure

Task ID: G10-090-RMI-IIOP-COMPATIBILITY-CLOSURE
Status: blocked
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-RMI-001, REQ-IDLJ-002, REQ-IIOP-001, REQ-NATIVE-002, REQ-INTEROP-001, REQ-INTEROP-003, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0007, ADR-0008, ADR-0010, ADR-0013
Specification references: JAV2I-14, JAV2I-14-RMI-IDL, CORBA-IOP-CDR, CORBA-IOP-GIOP, CORBA-IOP-IIOP
Target module: modules/corba-rmi-iiop, modules/corba-idl-java-mapping, modules/corba-codegen, modules/corba-cdr, modules/corba-giop, modules/corba-iiop
Allowed files: modules/corba-rmi-iiop/src/**, modules/corba-idl-java-mapping/src/**, modules/corba-codegen/src/**, modules/corba-cdr/src/**, modules/corba-giop/src/**, modules/corba-iiop/src/**, docs/architecture/rmi-iiop-java-to-idl.md, docs/conformance/corba-2.3-legacy-java-matrix.md, docs/conformance/idl-to-java-matrix.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-090-rmi-iiop-compatibility-closure.md, docs/roadmap/tasks/g10-100-native-image-interop-binaries.md, README.md
Forbidden files: Java serialization marshaling without a dedicated ADR, peer artifacts, live interop reports, optional service implementation, classpath scanning
Expected behavior: Task type: implementation. Expand RMI-IIOP beyond the current primitive/wstring/void/empty-exception slice to peer-facing Java-to-IDL compatibility: value types, object references, repository IDs, user exceptions with payloads, inheritance, generated stubs/ties/skeletons, and code-set behavior.
Tests to add/update: RMI eligibility, mapping, repository ID, generated IDL/Java, CDR, GIOP/IIOP loopback, hostile-input, Native Image smoke, and interop report tests.
Documentation to update: RMI-IIOP architecture, legacy Java/CORBA and IDL-to-Java matrices, interop matrix, Native Image matrix, roadmap index, README ready-task status, this task, and G10-100 status when complete.
Commands to run: ./gradlew :modules:corba-rmi-iiop:test :modules:corba-idl-java-mapping:test :modules:corba-codegen:test :modules:corba-cdr:test :modules:corba-giop:test :modules:corba-iiop:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: This task remains blocked until G10-020 and G10-040 are complete; RMI-IIOP peer scenarios are fully represented in local generated binding, CDR, and wire behavior without unapproved serialization, scanning, or reflection metadata.
Rollback notes: Revert RMI-IIOP, mapping/codegen, wire, test, and documentation changes together.
