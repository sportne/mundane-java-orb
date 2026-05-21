# G7-080 RMI-IIOP Wire Integration

Task ID: G7-080-RMI-IIOP-WIRE-INTEGRATION
Status: ready-for-implementation
Gate: G7 RMI-IIOP and Java-to-IDL
Requirement IDs: REQ-RMI-001, REQ-GIOP-001, REQ-IIOP-001, REQ-CDR-001, REQ-SEC-001, REQ-SEC-002, REQ-SEC-003
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010, ADR-0013
Specification references: JAV2I-14-RMI-IDL, CORBA-IOP-GIOP, CORBA-IOP-IIOP, CORBA-IOP-CDR
Target module: modules/corba-rmi-iiop, modules/corba-giop, modules/corba-iiop, modules/corba-orb-core
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g7-080-rmi-iiop-wire-integration.md, docs/roadmap/tasks/g7-090-rmi-iiop-peer-interop.md, docs/architecture/rmi-iiop-java-to-idl.md, docs/architecture/cdr-giop-iiop.md, docs/architecture/runtime-architecture.md, docs/conformance/corba-2.3-legacy-java-matrix.md, modules/corba-rmi-iiop/README.md, modules/corba-rmi-iiop/src/main/**, modules/corba-rmi-iiop/src/test/**, modules/corba-giop/src/main/**, modules/corba-giop/src/test/**, modules/corba-iiop/src/main/**, modules/corba-iiop/src/test/**, modules/corba-orb-core/src/main/**, modules/corba-orb-core/src/test/**
Forbidden files: external peer artifact downloads, live peer execution, optional services, dynamic proxies, reflection-driven invocation, Java serialization marshaling, unbounded network allocation behavior
Expected behavior: Task type: implementation. Integrate approved RMI-IIOP request/reply behavior with bounded GIOP/IIOP paths for local JVM wire scenarios.
Tests to add/update: Unit, loopback integration, golden-wire, negative, timeout, malformed request/reply, and hostile-input tests for approved RMI-IIOP wire slices.
Documentation to update: CDR/GIOP/IIOP architecture notes, runtime architecture, RMI-IIOP README/package docs, conformance rows, roadmap index, this task, and G7-090 status.
Commands to run: ./gradlew :modules:corba-rmi-iiop:test :modules:corba-giop:test :modules:corba-iiop:test :modules:corba-orb-core:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Approved RMI-IIOP calls pass over bounded local GIOP/IIOP request/reply paths, malformed inputs fail deterministically, and no external peer compatibility is claimed.
Rollback notes: Revert wire integration, tests, docs, and roadmap status updates together.
