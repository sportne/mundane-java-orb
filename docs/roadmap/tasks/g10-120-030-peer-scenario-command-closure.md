# G10-120-030 Peer Scenario Command Closure

Task ID: G10-120-030-PEER-SCENARIO-COMMAND-CLOSURE
Status: complete
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-008, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0013
Specification references: CORBA-IF, CORBA-IOP, IDL-42, I2JAV-13, JAV2I-14
Target module: interop peer command surfaces
Allowed files: interop/**, modules/corba-interop-testkit/src/**, modules/corba-cdr/src/**, modules/corba-giop/src/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-120-030-peer-scenario-command-closure.md, docs/roadmap/tasks/g10-120-040-live-direction-matrix-evidence.md, README.md
Forbidden files: Gradle build logic changes, vendored peer source, committed peer binaries, committed TAO-generated C++ sources, optional service implementation, unapproved release version changes
Expected behavior: Task type: implementation. Make peer commands scenario-aware for the non-optional G10-120 lanes. ACE/TAO must implement the `rmi-iiop` Calculator lane by generating TAO C++ bindings inside the ignored image build context from `interop/idl/rmi-iiop/Calculator.idl` and running clean-room servant/client glue.
Tests to add/update: Interop testkit coverage for scenario-aware Java peer smoke, ACE/TAO rmi-iiop command mapping, prevention of committed generated peer artifacts, and structured unsupported/missing-prerequisite outcomes for genuinely unavailable peer behavior.
Documentation to update: ACE/TAO README, interop matrix, reference behavior capture, pre-1.0 interop plan, roadmap index, README, this task, and G10-120-040 status when complete.
Commands to run: ./gradlew :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; INTEROP_ARTIFACT_CACHE=/mnt/d/projects/mundane-java-orb/interop/work/artifact-cache ./interop/bin/interop-peer validate-gates --require-cache; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: ACE/TAO `rmi-iiop` no longer reports `unsupported-scenario`; `run-scenario --require-live rmi-iiop ace-tao` passes after rebuilding the approved image; peer scenario commands do not commit third-party source, generated C++ bindings, binaries, or raw live reports.
Completion evidence: Completed on 2026-05-25. The approved ACE/TAO image now generates Calculator C++ bindings during the image build from `interop/idl/rmi-iiop/Calculator.idl` and runs clean-room Calculator servant/client glue for `add`, `describe`, `CalculatorProblem`, and `clear`. Live execution passed `./interop/bin/interop-peer run-scenario --require-live rmi-iiop ace-tao` and `./interop/bin/interop-peer run-direction-matrix --require-live rmi-iiop ace-tao` with rebuilt JVM/native lane binaries. The live TAO run exposed and closed two narrow wire compatibility gaps: GIOP 1.2 operation bodies are now aligned to the message origin, and CDR `wstring` values use BOM-prefixed UTF-16 octet lengths while retaining legacy local decode compatibility.
Rollback notes: Revert peer scenario command glue and restore prior structured deferral behavior.
