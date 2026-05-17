# G6-520 IIOP TCP

Task ID: G6-520-IIOP-TCP
Status: complete
Gate: G6 wire invocation vertical slice
Requirement IDs: REQ-IIOP-001, REQ-GIOP-001, REQ-SEC-001, REQ-NFR-004, REQ-DOC-004
ADR IDs: ADR-0002, ADR-0004, ADR-0005, ADR-0010
Specification references: CORBA-IOP-IIOP, CORBA-IOP-GIOP
Target module: modules/corba-iiop
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-520-iiop-tcp.md, docs/roadmap/tasks/g6-530-iiop-tls-mtls.md, docs/architecture/cdr-giop-iiop.md, docs/conformance/**, modules/corba-iiop/build.gradle, modules/corba-iiop/README.md, modules/corba-iiop/src/**, modules/corba-giop/src/main/**
Forbidden files: TLS/mTLS behavior, external peer interop, optional services
Expected behavior: Task type: implementation. Add local TCP client/server transport for bounded GIOP request/reply correlation in the Hello slice.
Tests to add/update: Integration tests for loopback client/server calls, timeouts, backpressure basics, and clean shutdown.
Documentation to update: IIOP design notes and conformance rows.
Commands to run: ./gradlew :modules:corba-iiop:test :modules:corba-giop:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Hello slice can execute over local loopback IIOP without external ORBs.
Rollback notes: Revert IIOP TCP implementation, tests, and docs together.
