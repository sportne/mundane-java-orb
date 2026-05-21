# G7-070 Local RMI-IIOP Adapters

Task ID: G7-070-LOCAL-RMI-IIOP-ADAPTERS
Status: complete
Gate: G7 RMI-IIOP and Java-to-IDL
Requirement IDs: REQ-RMI-001, REQ-ORB-001, REQ-POA-001, REQ-IDLJ-004, REQ-NFR-001
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010, ADR-0013
Specification references: JAV2I-14-RMI-IDL, CORBA-IF-ORB, CORBA-IF-POA
Target module: modules/corba-rmi-iiop, modules/corba-orb-core, modules/corba-poa
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g7-070-local-rmi-iiop-adapters.md, docs/roadmap/tasks/g7-080-rmi-iiop-wire-integration.md, docs/architecture/rmi-iiop-java-to-idl.md, docs/architecture/runtime-architecture.md, docs/architecture/poa-design.md, docs/conformance/corba-2.3-legacy-java-matrix.md, docs/conformance/idl-to-java-matrix.md, modules/corba-rmi-iiop/README.md, modules/corba-rmi-iiop/build.gradle, modules/corba-rmi-iiop/src/main/**, modules/corba-rmi-iiop/src/test/**, modules/corba-orb-core/src/main/**, modules/corba-orb-core/src/test/**, modules/corba-poa/src/main/**, modules/corba-poa/src/test/**
Forbidden files: IIOP socket behavior, peer interop execution, dynamic proxies, reflection-driven invocation, runtime bytecode generation, Java serialization marshaling, unrelated ORB or POA features
Expected behavior: Task type: implementation. Add local ORB/POA RMI-IIOP adapter invocation for approved generated binding surfaces before any external wire compatibility claim.
Tests to add/update: Unit and local integration tests for adapter registration, local object reference identity, invocation dispatch, declared exception propagation, shutdown behavior, and unsupported operation diagnostics.
Documentation to update: Runtime architecture, POA design notes, RMI-IIOP README/package docs, conformance rows, roadmap index, this task, and G7-080 status.
Commands to run: ./gradlew :modules:corba-rmi-iiop:test :modules:corba-orb-core:test :modules:corba-poa:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Approved RMI-IIOP bindings can invoke local adapters through ORB/POA paths without opening sockets or adding reflection-driven invocation.
Rollback notes: Revert local adapter implementation, tests, docs, and roadmap status updates together.
