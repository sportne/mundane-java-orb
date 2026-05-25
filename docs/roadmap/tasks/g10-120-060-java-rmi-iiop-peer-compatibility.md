# G10-120-060 Java RMI-IIOP Peer Compatibility

Task ID: G10-120-060-JAVA-RMI-IIOP-PEER-COMPATIBILITY
Status: ready-for-implementation
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-008, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0013
Specification references: CORBA-IF, CORBA-IOP, IDL-42, I2JAV-13, JAV2I-14
Target module: Java ORB RMI-IIOP peer compatibility
Allowed files: interop/**, modules/corba-interop-testkit/src/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-120-*.md, README.md
Forbidden files: production behavior changes unless explicitly amended by G10-120-080, Gradle build logic changes, vendored peer source, committed peer binaries, raw live report outputs, optional service implementation, unapproved release version changes
Expected behavior: Task type: implementation. Close Java ORB RMI-IIOP live-lane gaps exposed by GlassFish, JacORB, and JBoss by first attempting compatibility fixes in interop peer glue and local smoke lanes, including code-set negotiation evidence, wchar/wstring behavior, DII request/return behavior, and Calculator exception reporting.
Tests to add/update: Interop testkit coverage for Java RMI-IIOP peer command behavior and structured ownership evidence for any remaining project-owned defect.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interop plan, roadmap index, README, this task, and G10-120-070 status when complete.
Commands to run: ./gradlew :modules:corba-interop-testkit:test :modules:corba-native-image:test; ./interop/bin/interop-peer validate-manifests; INTEROP_ARTIFACT_CACHE=/mnt/d/projects/mundane-java-orb/interop/work/artifact-cache ./interop/bin/interop-peer validate-gates --require-cache; ./gradlew validateDesignControlPack qualityGate; ./interop/bin/interop-peer run-direction-matrix --require-live rmi-iiop jacorb; ./interop/bin/interop-peer run-direction-matrix --require-live rmi-iiop glassfish-orb; ./interop/bin/interop-peer run-direction-matrix --require-live rmi-iiop jboss-openjdk-orb; git diff --check
Acceptance criteria: Java peer `rmi-iiop` direction matrix lanes pass or produce structured ownership evidence with no unresolved infrastructure failures. Project-owned defects are not classified away and must be closed by G10-120-080.
Rollback notes: Revert Java RMI-IIOP peer compatibility glue, related testkit coverage, and roadmap status updates.
