# G6-530 IIOP TLS and mTLS

Task ID: G6-530-IIOP-TLS-MTLS
Gate: G6 wire invocation vertical slice
Requirement IDs: REQ-IIOP-002, REQ-SEC-005, REQ-DOC-004
ADR IDs: ADR-0002, ADR-0004, ADR-0005, ADR-0010
Specification references: CORBA-IOP-SECURITY, CORBA-IOP-IIOP
Target module: modules/corba-iiop
Allowed files: modules/corba-iiop/src/main/**, modules/corba-iiop/src/test/**, docs/architecture/cdr-giop-iiop.md
Forbidden files: global JVM TLS state mutation, peer artifact downloads, optional security service implementation
Expected behavior: Task type: implementation. Add explicit TLS and mTLS endpoint configuration for IIOP without relying on global JVM state.
Tests to add/update: Integration tests with generated local certificates and negative tests for trust and client-certificate failures.
Documentation to update: IIOP and security configuration docs.
Commands to run: ./gradlew :modules:corba-iiop:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: TLS settings are per-endpoint, deterministic, and covered by local tests.
Rollback notes: Revert TLS/mTLS implementation, tests, and docs together.

