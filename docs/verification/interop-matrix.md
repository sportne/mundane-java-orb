# Interoperability Matrix

## Peers

| Peer | Manifest | JVM peer | Native peer | Candidate origin | Gate status | Notes |
|---|---|---:|---:|---|---|---|
| JacORB | `interop/peers/jacorb/peer.yaml` | yes | no | `org.jacorb:jacorb:3.9` | G6-820 approved | Java ORB. External cache entry pinned in `interop/approvals/jacorb.approval.yaml`. |
| Eclipse GlassFish CORBA ORB | `interop/peers/glassfish-orb/peer.yaml` | yes | no | `org.glassfish.corba:glassfish-corba:5.0.0`, `eclipse-ee4j/orb` | G6-820 approved | Java/Jakarta ORB. External cache entry pinned in `interop/approvals/glassfish-orb.approval.yaml`. |
| JBoss OpenJDK ORB | `interop/peers/jboss-openjdk-orb/peer.yaml` | yes | no | `org.jboss.openjdk-orb:openjdk-orb:10.1.1.Final` | G6-820 approved | Legacy Java/OpenJDK ORB. External cache entry pinned in `interop/approvals/jboss-openjdk-orb.approval.yaml`. |
| ACE/TAO | `interop/peers/ace-tao/peer.yaml` | no | yes | `ACE+TAO-8.0.6` | G6-820 approved | C++ native ORB. External cache entry pinned in `interop/approvals/ace-tao.approval.yaml`. |

The G6-820 peer gates are dry-run executable and cache-verifiable. G6-830 adds
an environment-gated report harness on top of those gates: digest-pinned base
images, external artifact/cache inputs only, no vendored peer source or
binaries, test port `2809`, and logs, IORs, and structured reports as outputs.

## Gate validation

`interop/bin/interop-peer validate-manifests` checks the structural peer
manifests and their container command contract. `interop/bin/interop-peer
validate-gates` checks approval records, manifest-to-approval consistency,
clean-room controls, external cache layout, and SHA-256 pins. Add
`--require-cache` with an absolute `INTEROP_ARTIFACT_CACHE` to verify cached
artifacts before real peer image preparation or live container execution.
Supplying a peer name validates only that peer's approval and cache entries;
`build-image <peer>` and live run commands use that scoped validation.

## G6-830 report lane

The selected automated scenario for G6-830 is `basic-idl`. It exercises the
approved container command contract and proves that missing prerequisites and
peer command failures become structured reports instead of silent skips. The
initial live command shape is:

```bash
INTEROP_ARTIFACT_CACHE=/absolute/cache \
INTEROP_JAVA_BASE_IMAGE=example@sha256:... \
INTEROP_NATIVE_BASE_IMAGE=example@sha256:... \
interop/bin/interop-peer run-scenario --require-live basic-idl all
```

Reports are written to each peer manifest's `reports.structuredReports` path,
with command logs under `reports.logs` and IOR outputs under `reports.iors`.
Dry-run commands remain available for all peers and do not create outputs.

Real peer launch, health, and report commands are no longer blocked by roadmap
state alone. They validate gates first and then either run the configured
Docker/Podman container command or write a deterministic infrastructure-failure
report when required external cache, base image, image, or runtime prerequisites
are missing.

G6-930 closes the structured-report verification surface for missing cache
entries, missing digest-pinned base images, missing container runtimes, peer
command failures, dry-run non-mutation, and report summary generation. Live peer
success remains environment-gated by the approved external inputs above.

## G7-090 RMI-IIOP scenario lane

G7-090 adds `rmi-iiop` as the first RMI-IIOP peer scenario. The scenario uses
`interop/idl/rmi-iiop/Calculator.idl`, matching the approved G7 primitive,
`wstring`, `void`, and empty declared user-exception slice. The scenario is
declared for JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, and
applicable ACE/TAO checks.

The default local gate validates manifests, approval records, dry-run behavior,
missing-prerequisite structured reports, and report summaries. Live peer
execution remains optional and requires the same approved external cache,
digest-pinned base image, and container runtime inputs as the G6-830 report
lane:

```bash
INTEROP_ARTIFACT_CACHE=/absolute/cache \
INTEROP_JAVA_BASE_IMAGE=example@sha256:... \
INTEROP_NATIVE_BASE_IMAGE=example@sha256:... \
interop/bin/interop-peer run-scenario --require-live rmi-iiop all
```

G7-100 closes the default local RMI-IIOP report evidence: missing prerequisites
produce deterministic `infrastructure-failure` reports, dry runs remain
non-mutating, and summaries preserve the `rmi-iiop` scenario report paths. Live
peer pass/fail evidence remains environment-gated by the approved external
inputs above.

## Pre-1.0 full matrix gate

Full live interoperability evidence for `1.0.0` is blocked on the G10
pre-1.0 roadmap. G10 keeps optional CORBA Services deferred, but requires the
non-optional IDL, IDL-to-Java, OMG API, CDR/GIOP/IIOP/IOR, ORB/POA, Naming,
DynamicAny/DII/DSI/Interface Repository, Portable Interceptor, RMI-IIOP, Native
Image, and real peer harness closure tasks to complete before
`G10-120-PRE-1.0-FULL-INTEROP-EXECUTION` runs.

Until those prerequisite tasks complete, `basic-idl` and `rmi-iiop` remain the
only existing peer lanes, and live peer pass/fail results are not a 1.0.0
compatibility claim.

G10-060 adds deterministic local evidence for the Naming Service lane before
live peer execution: `NetworkNamingServiceTest` starts a bounded loopback IIOP
Naming Service, exchanges object and context IORs, resolves remote
`corbaloc`/`corbaname` URLs, and verifies missing-name classification without
external peers. Live Naming pass/fail evidence remains blocked until the G10
native-image, real-peer harness, and full execution tasks complete.

G10-070 adds deterministic local evidence for the DynamicAny/DII/DSI/Interface
Repository lane before live peer execution: `DynamicIiopInvocationCodecTest`
routes descriptor-backed dynamic requests and replies over loopback IIOP,
including object-reference Any values and user-exception diagnostics, while
`StaticInterfaceRepositoryTest` verifies bounded repository lookup and recursive
reference diagnostics. Live DynamicAny, DII, DSI, and Interface Repository
pass/fail evidence remains blocked until the G10 native-image, real-peer
harness, and full execution tasks complete.

## Required directions

| Client | Server | Required |
|---|---|---|
| our JVM client | our JVM server | yes |
| our native client | our native server | yes |
| our JVM client | our native server | yes |
| our native client | our JVM server | yes |
| our JVM client | peer server | yes |
| our native client | peer server | yes |
| peer client | our JVM server | yes |
| peer client | our native server | yes |

## Scenario groups

- basic primitives, structs, enums, strings, exceptions;
- IOR, stringified IOR, object references;
- corbaloc and corbaname;
- POA policies;
- Naming Service;
- Any and TypeCode;
- valuetypes;
- GIOP versions and fragmentation;
- code set negotiation;
- Portable Interceptors;
- RMI-IIOP;
- optional services only after each service gate defines its scenario, report
  fields, cache requirements, and live execution prerequisites.

G10-080 records deterministic local Portable Interceptor evidence for the
implemented ORB/IIOP loopback path: client/server request-flow ordering,
service-context propagation, and callback failure diagnostics. Live peer
Portable Interceptor pass/fail evidence remains blocked until the G10 peer
harness and full execution tasks complete.

G10-090 records deterministic local RMI-IIOP compatibility evidence before
live peer execution: explicit Java-to-IDL models and generated bindings preserve
remote-interface inheritance, bounded CDR payloads carry sequences, remote
object-reference keys, declared-value members, and user-exception payload
fields, and the existing loopback wire path returns those payloads without Java
serialization, classpath scanning, or reflection metadata. Live RMI-IIOP
pass/fail evidence remains blocked until the G10 native-image, peer harness,
and full execution tasks complete.

G10-100 adds deterministic local Native Image evidence before live peer
execution. The native-image smoke suite builds and runs aggregate
`interopClient` and `interopServer` binaries for the completed local G10 lanes
when GraalVM Native Image is available. The interop CLI also records structured
native-lane `infrastructure-failure` reports when required native client or
server binaries are missing. Live peer pass/fail evidence remains blocked until
the G10 real-peer harness and full execution tasks complete.

G10-110 closes the real-peer harness prerequisite for live execution. The
harness validates approved external cache entries, digest-pinned base-image
inputs, Docker/Podman availability, prepared peer images, real peer command
entrypoints, mounted scenario IDL, IOR/log/report directories, detached server
lifecycle, health checks, cleanup, and clean-room failure classification before
running JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, or ACE/TAO as
black-box peers. Live pass/fail evidence remains a G10-120 verification task,
not a compatibility claim from harness closure alone.

The first G10-120 execution attempt on 2026-05-24 did not reach live peer
behavior. `validate-gates --require-cache` failed because
`INTEROP_ARTIFACT_CACHE` was unset and approved cache entries were unavailable.
`run-scenario --require-live basic-idl all` produced structured
`infrastructure-failure` reports for all four peer server lanes at prerequisite
validation.

A follow-up approved-cache attempt validated the repo-local ignored artifact
cache, built Native Image client/server smoke binaries with the SDKMAN GraalVM
toolchain, and reached `basic-idl` peer-command smoke success for JacORB, JBoss
OpenJDK ORB, and Eclipse GlassFish CORBA ORB using ignored local peer images.
The uncommitted prerequisite reports were generated under
`build/interop/jacorb/reports/`, `build/interop/jboss-openjdk-orb/reports/`, and
`build/interop/glassfish-orb/reports/`. That result remains prerequisite
evidence only. ACE/TAO now has a tracked peer-specific image definition and
clean-room command sources. On 2026-05-25, the approved ACE/TAO follow-up built
the local peer image from the approved cache archive and ran the `basic-idl`
peer smoke through server, health, client, and summary commands on an explicit
local Docker network. The original scenario runner starts peer server/client
roles without exercising the required our-JVM/our-native versus peer directions.
Maintainer approval on 2026-05-24 allowed the harness direction work needed to
make those prerequisites explicit. `run-direction-matrix` now starts selected
peer servers before our JVM/native client lanes, and starts our JVM/native
server lanes before selected peer clients. Full pre-1.0 live interop remains
blocked until the required local peer-facing commands and complete approved
live scenario execution are available.

The remaining G10-120 work is tracked as child tasks. G10-120-020 adds the
required local JVM/native direction commands, G10-120-030 makes peer commands
scenario-aware and includes ACE/TAO `rmi-iiop` Calculator execution, and
G10-120-040 records final live direction-matrix evidence.

## Optional service lanes

G6-D20 splits Trading, Event, Notification, Transaction, Security, and Time
Services into separate human-gated design tasks. No optional service currently
has a live peer lane, structured report schema, or pass/fail evidence. Future
service gates must add those details before any peer execution is required.
