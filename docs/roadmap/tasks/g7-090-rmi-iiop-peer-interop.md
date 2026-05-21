# G7-090 RMI-IIOP Peer Interop

Task ID: G7-090-RMI-IIOP-PEER-INTEROP
Status: complete
Gate: G7 RMI-IIOP and Java-to-IDL
Requirement IDs: REQ-RMI-001, REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0006, ADR-0010, ADR-0013
Specification references: JAV2I-14-RMI-IDL, CORBA-IOP-ARCH, CORBA-IOP-GIOP, CORBA-IOP-IIOP
Target module: modules/corba-rmi-iiop, modules/corba-interop-testkit, interop
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g7-090-rmi-iiop-peer-interop.md, docs/roadmap/tasks/g7-100-rmi-iiop-native-security-closure.md, docs/architecture/rmi-iiop-java-to-idl.md, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/conformance/corba-2.3-legacy-java-matrix.md, interop/**, modules/corba-rmi-iiop/src/test/**, modules/corba-interop-testkit/src/main/**, modules/corba-interop-testkit/src/test/**
Forbidden files: vendored peer source or binaries, unapproved external downloads, reference implementation source copying, optional services, unrelated protocol features, public API changes outside approved RMI-IIOP surfaces
Expected behavior: Task type: implementation. Add environment-gated RMI-IIOP peer interop scenarios for JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, and applicable ACE/TAO cross-language checks.
Tests to add/update: Interop harness tests, dry-run tests, missing-prerequisite report tests, structured failure report tests, peer scenario manifests, and environment-gated live scenario documentation.
Documentation to update: Interop matrix, reference behavior capture notes, conformance rows, RMI-IIOP architecture notes, roadmap index, this task, and G7-100 status.
Commands to run: ./gradlew :modules:corba-rmi-iiop:test :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer validate-gates; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: RMI-IIOP peer scenarios are gate-validated and report structured pass/fail/infrastructure outcomes without vendoring peer code or requiring live external inputs for the default local gate.
Rollback notes: Revert interop scenarios, tests, reports, docs, and roadmap status updates together.
