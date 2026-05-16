# G2 Architecture Boundary Enforcement Handoff

```text
Task ID: G2-ARCH-BOUNDARY-ENFORCEMENT
Gate: G2 architecture preparation
Requirement IDs: REQ-BUILD-007, REQ-NFR-002, REQ-SEC-004
ADR IDs: ADR-0001, ADR-0004, ADR-0008, ADR-0010
Specification references: Architecture-enforcement task; no direct OMG clause.
Target module: modules/corba-architecture-tests
Allowed files:
- modules/corba-architecture-tests/src/test/java/io/corbaecosystem/architecture/ArchitectureRulesTest.java
- docs/architecture/module-boundaries.md
- docs/agent/agent-handoff.md
- docs/agent/g2-architecture-boundary-handoff.md
Forbidden files:
- modules/**/src/main/**
- runtime, protocol, IDL, ORB, POA, service, compiler, or generated behavior
- Gradle dependency or plugin configuration unless validation proves it is required
Expected behavior:
- Architecture tests enforce the documented module boundary rules that can be
  expressed at scaffold time.
- Empty package matches remain allowed until later gates introduce concrete
  implementation packages.
- Public APIs, interfaces, and types remain unchanged.
Tests to add/update:
- Expand ArchUnit/JUnit checks for org.omg.* ownership, IDL isolation, CDR,
  GIOP, IIOP, protocol/core separation, reflection, Java serialization, and
  runtime bytecode-generation restrictions.
Documentation to update:
- Record the staged enforcement coverage in docs/architecture/module-boundaries.md.
- Update docs/agent/agent-handoff.md with the completed G2 task and remaining
  open setup work.
Commands to run:
- ./gradlew :modules:corba-architecture-tests:test
- ./gradlew validateDesignControlPack qualityGate
- git diff --check
Acceptance criteria:
- All listed commands pass.
- git diff --name-only is limited to the allowed files above.
- No production source, runtime behavior, protocol behavior, IDL behavior,
  compiler behavior, or generated code is added.
Rollback notes:
- Revert this handoff, the module-boundary enforcement note, the agent handoff
  note, and the ArchitectureRulesTest changes together.
```
