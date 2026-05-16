# G3 Interop Peer Manifest Handoff

```text
Task ID: G3-INTEROP-PEER-MANIFEST-CLOSURE
Gate: G3 verification planning
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003,
  REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007,
  REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-005
ADR IDs: ADR-0001, ADR-0006, ADR-0010
Specification references: CORBA-IOP-ARCH, CORBA-IOP-GIOP, CORBA-IOP-IIOP,
  CORBA-IOP-IOR, CORBA-IOP-OBJECT-URL
Target area: interop peer manifests
Allowed files:
- interop/README.md
- interop/peers/*/README.md
- interop/peers/*/peer.yaml
- docs/verification/interop-matrix.md
- docs/agent/agent-handoff.md
- docs/agent/g3-interop-peer-manifest-handoff.md
Forbidden files:
- modules/**/src/main/**
- runtime, protocol, IDL, ORB, POA, service, compiler, or generated behavior
- Dockerfiles, launch scripts, Gradle dependency/plugin changes, and downloaded
  peer artifacts
Expected behavior:
- Peer manifests identify JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK
  ORB, and ACE/TAO candidate versions, license review state, clean-room limits,
  future container inputs, launch expectations, and report outputs.
- G3 remains non-executable. G4 must implement deterministic container builds,
  launch scripts, health checks, and report generation from these manifests.
Tests to add/update:
- None. This is documentation and manifest closure only.
Documentation to update:
- Refresh interop README files, the interop matrix, and this agent handoff.
Commands to run:
- ./gradlew validateDesignControlPack qualityGate
- git diff --check
- python3 - <<'PY'
  import glob, yaml
  for path in glob.glob('interop/peers/*/peer.yaml'):
      with open(path, encoding='utf-8') as handle:
          yaml.safe_load(handle)
      print(path)
  PY
Acceptance criteria:
- All listed commands pass.
- git diff --name-only is limited to the allowed files above.
- No production source, runtime behavior, protocol behavior, IDL behavior,
  compiler behavior, generated code, Dockerfile, launch script, or peer artifact
  is added.
Rollback notes:
- Revert the peer manifests, interop README updates, interop matrix update, and
  handoff updates together.
```
