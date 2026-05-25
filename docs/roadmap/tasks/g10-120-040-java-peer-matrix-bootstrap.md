# G10-120-040 Java Peer Matrix Bootstrap

Task ID: G10-120-040-JAVA-PEER-MATRIX-BOOTSTRAP
Status: complete
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-002
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0013
Specification references: CORBA-IF, CORBA-IOP, IDL-42, I2JAV-13, JAV2I-14, NAM-13
Target module: Java peer bootstrap and interop verification
Allowed files: interop/**, modules/corba-interop-testkit/src/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/conformance/*.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-120-*.md, README.md
Forbidden files: production behavior changes, Gradle build logic changes, vendored peer source, committed peer binaries, committed native binaries, raw live report outputs, optional service implementation, unapproved release version changes
Expected behavior: Task type: implementation. Add missing mounted IDL fixtures for `object-reference`, `naming`, `giop`, and `iiop`; make the generic Java peer smoke scenario-aware for liveness and Calculator RMI-IIOP DSI paths; harden Java peer endpoint/listen configuration for JacORB, GlassFish, and JBoss without changing production behavior.
Tests to add/update: Interop testkit coverage for fixture presence, Java peer Calculator behavior, endpoint policy, and prevention of committed peer artifacts.
Documentation to update: Roadmap index, README, parent G10-120 task, and follow-on G10-120 child task status.
Commands to run: ./gradlew :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; INTEROP_ARTIFACT_CACHE=/mnt/d/projects/mundane-java-orb/interop/work/artifact-cache ./interop/bin/interop-peer validate-gates --require-cache; ./gradlew validateDesignControlPack qualityGate; git diff --check; live basic-IDL and non-RMI smoke lane reruns for ACE/TAO, JacORB, and GlassFish.
Acceptance criteria: `basic-idl` and non-RMI smoke lanes pass for ACE/TAO, JacORB, and GlassFish; JBoss failures are reduced to the dedicated readiness task.
Completion evidence: Completed on 2026-05-25. The Java peer smoke now mounts all declared non-RMI scenario IDL fixtures, starts scenario-aware DSI servants, exposes stable endpoint/listen defaults for JacORB, GlassFish, and JBoss, and provides a Calculator DSI lane for later RMI-IIOP compatibility work. The interop harness now copies peer-server IOR fallback data through a temporary file so a bind-mounted IOR is not truncated during readiness checks. Focused testkit coverage passed, required design and quality gates passed, and live `run-direction-matrix --require-live` smoke reruns passed for `basic-idl`, `object-reference`, `naming`, `giop`, and `iiop` against ACE/TAO, JacORB, and GlassFish. The task leaves JBoss readiness and deeper Java RMI-IIOP compatibility to G10-120-050 and G10-120-060.
Rollback notes: Revert Java peer bootstrap wiring, mounted scenario IDL fixtures, testkit assertions, and roadmap status updates.
