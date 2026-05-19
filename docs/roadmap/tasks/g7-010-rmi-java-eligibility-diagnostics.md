# G7-010 RMI Java Eligibility Diagnostics

Task ID: G7-010-RMI-JAVA-ELIGIBILITY-DIAGNOSTICS
Status: complete
Gate: G7 RMI-IIOP and Java-to-IDL
Requirement IDs: REQ-RMI-001, REQ-IDLJ-002, REQ-IDLJ-004, REQ-SEC-004, REQ-DOC-001
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010, ADR-0013
Specification references: JAV2I-14-RMI-IDL
Target module: modules/corba-rmi-iiop
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g7-010-rmi-java-eligibility-diagnostics.md, docs/roadmap/tasks/g7-020-java-to-idl-model.md, docs/architecture/rmi-iiop-java-to-idl.md, docs/conformance/corba-2.3-legacy-java-matrix.md, docs/conformance/idl-to-java-matrix.md, modules/corba-rmi-iiop/build.gradle, modules/corba-rmi-iiop/README.md, modules/corba-rmi-iiop/src/main/**, modules/corba-rmi-iiop/src/test/**
Forbidden files: ORB invocation, IIOP wire behavior, generated source emission, stubs, ties, skeletons, value marshaling, exception marshaling, dynamic proxies, reflection-driven runtime invocation, classpath scanning, Java serialization marshaling, external Gradle dependency changes, peer interop execution
Expected behavior: Task type: implementation. Implement an explicit Java-to-IDL eligibility model and deterministic diagnostics for supported and unsupported Java remote-interface declaration shapes without loading application classes at runtime.
Tests to add/update: Unit tests for accepted remote-interface declarations, rejected non-remote declarations, unsupported methods, unsupported type references, deterministic diagnostic codes, null/blank input rejection, and Native Image forbidden-mechanism audits.
Documentation to update: Module README, package docs, RMI-IIOP architecture notes, conformance rows, roadmap index, this task, and G7-020 status.
Commands to run: ./gradlew :modules:corba-rmi-iiop:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Eligibility results and diagnostics are deterministic, no runtime reflection/classpath scanning/dynamic proxy/serialization path is introduced, unsupported inputs fail with stable diagnostics, and no IDL generation or ORB invocation behavior is added.
Rollback notes: Revert eligibility model, diagnostics, tests, docs, and roadmap status updates together.
