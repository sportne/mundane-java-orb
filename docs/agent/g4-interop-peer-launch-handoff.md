# G4 Interop Peer Launch Handoff

```text
Task ID: G4-INTEROP-PEER-LAUNCH-INFRASTRUCTURE
Gate: G4 build and verification infrastructure
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003,
  REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007,
  REQ-INTEROP-008, REQ-INTEROP-009, REQ-BUILD-009, REQ-NATIVE-005
ADR IDs: ADR-0001, ADR-0004, ADR-0006, ADR-0010
Specification references: CORBA-IOP-ARCH, CORBA-IOP-GIOP, CORBA-IOP-IIOP,
  CORBA-IOP-IOR, CORBA-IOP-OBJECT-URL
Target area: interop peer launch scaffolding
Allowed files:
- interop/README.md
- interop/bin/**
- interop/container/**
- interop/peers/*/README.md
- interop/peers/*/*.sh
- interop/peers/*/peer.yaml
- docs/verification/interop-matrix.md
- docs/agent/agent-handoff.md
- docs/agent/g4-interop-peer-launch-handoff.md
Forbidden files:
- modules/**/src/main/**
- runtime, protocol, IDL, ORB, POA, service, compiler, or generated behavior
- real interop assertions, Gradle dependency/plugin changes, committed peer
  source, committed peer binaries, and committed container build outputs
Expected behavior:
- interop/bin/interop-peer is the canonical CLI for help, manifest validation,
  dry-run image builds, dry-run launch plans, dry-run health checks, and dry-run
  report planning.
- Peer wrapper scripts delegate to the canonical CLI.
- Container templates remain scaffold-only and require digest-pinned base image
  values before real builds are allowed.
- Real peer launch, real health checks, artifact resolution, and clean-room
  report capture remain future tasks.
Tests to add/update:
- Shell syntax checks for interop scripts.
- CLI and wrapper dry-run smoke checks for each peer.
- PyYAML parse and manifest validation.
Documentation to update:
- Refresh interop README files, the interop matrix, and this agent handoff.
Commands to run:
- find interop -name '*.sh' -o -path 'interop/bin/*' | xargs bash -n
- ./interop/bin/interop-peer help
- ./interop/bin/interop-peer validate-manifests
- ./interop/bin/interop-peer build-image --dry-run jacorb
- ./interop/bin/interop-peer launch --dry-run jacorb server
- ./interop/bin/interop-peer health --dry-run jacorb
- ./interop/bin/interop-peer report --dry-run jacorb
- ./gradlew validateDesignControlPack qualityGate
- git diff --check
Acceptance criteria:
- All listed commands pass.
- Wrapper dry-run commands pass for jacorb, glassfish-orb, jboss-openjdk-orb,
  and ace-tao.
- git diff --name-only is limited to the allowed files above.
- No production source, runtime behavior, protocol behavior, IDL behavior,
  compiler behavior, generated code, real interop assertion, peer artifact, or
  container build output is added.
Rollback notes:
- Revert the CLI, peer wrappers, container templates, manifest updates, README
  updates, interop matrix update, and handoff updates together.
```
