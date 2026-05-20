# G7-060 RMI Value and Exception Marshaling

Task ID: G7-060-RMI-VALUE-EXCEPTION-MARSHALING
Status: complete
Gate: G7 RMI-IIOP and Java-to-IDL
Requirement IDs: REQ-RMI-001, REQ-CDR-001, REQ-IDLJ-004, REQ-SEC-001, REQ-SEC-002, REQ-SEC-004
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010, ADR-0013
Specification references: JAV2I-14-RMI-IDL, CORBA-IOP-CDR, CORBA-IF-VALUES
Target module: modules/corba-rmi-iiop, modules/corba-cdr, modules/corba-codegen
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g7-060-rmi-value-exception-marshaling.md, docs/roadmap/tasks/g7-070-local-rmi-iiop-adapters.md, docs/architecture/rmi-iiop-java-to-idl.md, docs/architecture/cdr-giop-iiop.md, docs/conformance/corba-2.3-legacy-java-matrix.md, docs/conformance/idl-to-java-matrix.md, modules/corba-rmi-iiop/build.gradle, modules/corba-rmi-iiop/README.md, modules/corba-rmi-iiop/src/main/**, modules/corba-rmi-iiop/src/test/**, modules/corba-cdr/src/main/**, modules/corba-cdr/src/test/**, modules/corba-codegen/src/main/**, modules/corba-codegen/src/test/**
Forbidden files: Java serialization marshaling, dynamic runtime class loading, ORB network invocation, IIOP socket behavior, peer interop execution, unbounded allocation behavior
Expected behavior: Task type: implementation. Implement bounded CDR-safe value and exception mapping for approved RMI-IIOP binding surfaces without using Java serialization as the marshaling mechanism.
Tests to add/update: Unit, negative, golden-wire, hostile-input, and bounds tests for value payloads, exception payloads, nullability, declared/undeclared exceptions, oversized inputs, and deterministic diagnostics.
Documentation to update: CDR/GIOP/IIOP architecture notes, RMI-IIOP README/package docs, conformance rows, roadmap index, this task, and G7-070 status.
Commands to run: ./gradlew :modules:corba-rmi-iiop:test :modules:corba-cdr:test :modules:corba-codegen:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Approved RMI-IIOP value and exception payloads round-trip through bounded CDR paths, hostile inputs fail deterministically, and Java serialization marshaling is absent.
Rollback notes: Revert marshaling implementation, tests, docs, and roadmap status updates together.
