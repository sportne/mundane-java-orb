# G7-100 RMI-IIOP Native and Security Closure

Task ID: G7-100-RMI-IIOP-NATIVE-SECURITY-CLOSURE
Status: complete
Gate: G7 RMI-IIOP and Java-to-IDL
Requirement IDs: REQ-RMI-001, REQ-NATIVE-001, REQ-NATIVE-002, REQ-NATIVE-003, REQ-NATIVE-004, REQ-NATIVE-005, REQ-SEC-001, REQ-SEC-002, REQ-SEC-003, REQ-SEC-004, REQ-INTEROP-009, REQ-NFR-004, REQ-NFR-005, REQ-NFR-007
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0010, ADR-0013
Specification references: JAV2I-14-RMI-IDL, CORBA-IOP-CDR, CORBA-IOP-GIOP, CORBA-IOP-IIOP
Target module: whole repository
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g7-100-rmi-iiop-native-security-closure.md, docs/architecture/rmi-iiop-java-to-idl.md, docs/conformance/**, docs/verification/**, modules/corba-rmi-iiop/src/test/**, modules/corba-native-image/**, modules/corba-interop-testkit/src/test/**, interop/**
Forbidden files: new RMI-IIOP feature behavior, unapproved generated artifacts, quality-gate weakening, Native Image policy weakening, reference implementation source copying, vendored peer binaries
Expected behavior: Task type: verification-only. Close G7 RMI-IIOP Native Image, hostile-input, conformance, interop-report, and release-hardening evidence after implementation slices land.
Tests to add/update: Native Image smoke tests, hostile-input tests, structured interop failure tests, conformance evidence checks, repeated deterministic smoke tests, and metadata-policy audits.
Documentation to update: RMI-IIOP architecture notes, conformance matrices, Native Image matrix, interop matrix, verification strategy, release-hardening closure evidence, roadmap index, and this task.
Commands to run: ./gradlew :modules:corba-rmi-iiop:test :modules:corba-native-image:test :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer validate-gates; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: RMI-IIOP conformance and verification records match implemented behavior, Native Image metadata remains explicit and reviewed, hostile inputs fail deterministically, and remaining gaps are explicitly deferred.
Rollback notes: Revert G7 closure tests, verification docs, conformance docs, and roadmap status updates together.
