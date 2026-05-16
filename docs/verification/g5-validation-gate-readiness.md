# G5 Validation Gate Readiness

This document records scaffold readiness evidence for maintainer review before
any G6 implementation task is approved. It does not approve G0 through G5 and
does not start CORBA runtime, protocol, IDL, ORB, POA, service, or compiler
implementation.

## Readiness evidence

| Area | Evidence | G5 readiness status |
|---|---|---|
| Design control | Charter, scope, ADRs, requirements, conformance matrices, and specification traceability are present. | Ready for maintainer review. |
| Agent governance | Task template and gate handoffs define allowed files, forbidden files, commands, and acceptance criteria. | Ready for maintainer review. |
| Architecture boundaries | Module boundary rules are documented and enforced by staged ArchUnit tests. | Ready with scaffold tolerance. |
| Build gates | `validateDesignControlPack`, `checkAll`, and `qualityGate` are documented local entry points. | Ready for maintainer review. |
| Coverage policy | Target thresholds are documented for implementation modules. | Ready with scaffold threshold deferral. |
| Offline build posture | Offline-build validation command and release validation expectations are documented. | Ready for later isolated execution. |
| Native Image posture | Required GraalVM toolchains, binaries, and test levels are documented. | Ready for later execution. |
| Interop scaffolding | Peer manifests and dry-run launch scaffolding are present for the selected Java and native peers. | Ready with artifact and execution deferrals. |

## Deferred tolerances

The following items are intentional scaffold tolerances and must not be treated
as G5 implementation work:

- ArchUnit rules allow empty package matches until implementation packages exist.
- JaCoCo verification uses permissive scaffold behavior until compiled
  production classes exist.
- External peer artifact resolution is not implemented.
- Real ORB peer launch and interop assertions are not implemented.
- Native Image binaries are not built or executed.
- License, legal, and dependency policy approval remain human gate items.
- Requirement statuses remain `draft` unless maintainers explicitly approve
  them in a separate gate action.

## Required validation commands

```bash
./gradlew validateDesignControlPack qualityGate
./gradlew :modules:corba-architecture-tests:test
./interop/bin/interop-peer validate-manifests
for peer in jacorb glassfish-orb jboss-openjdk-orb ace-tao; do
  interop/peers/${peer}/build-image.sh --dry-run
  interop/peers/${peer}/launch.sh --dry-run server
  interop/peers/${peer}/health.sh --dry-run
done
git diff --check
```

## Approval boundary

G5 readiness closure is evidence capture only. Maintainers must approve G0
through G5 before any G6 implementation handoff may add CORBA behavior,
published APIs, protocol handling, IDL parsing, code generation, real interop
assertions, or Native Image execution.
