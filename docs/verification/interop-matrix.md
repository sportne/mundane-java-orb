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

The G10 pre-1.0 roadmap keeps optional CORBA Services deferred, but required
the non-optional IDL, IDL-to-Java, OMG API, CDR/GIOP/IIOP/IOR, ORB/POA, Naming,
DynamicAny/DII/DSI/Interface Repository, Portable Interceptor, RMI-IIOP, Native
Image, and real peer harness closure tasks to complete before
`G10-120-PRE-1.0-FULL-INTEROP-EXECUTION` could run. Those prerequisites and the
final G10-120 live matrix are complete.

G10-060 adds deterministic local evidence for the Naming Service lane:
`NetworkNamingServiceTest` starts a bounded loopback IIOP
Naming Service, exchanges object and context IORs, resolves remote
`corbaloc`/`corbaname` URLs, and verifies missing-name classification without
external peers. G10-120 adds live peer pass/fail evidence for the approved
`naming` scenario across all approved peers and JVM/native local lanes.

G10-070 adds deterministic local evidence for the DynamicAny/DII/DSI/Interface
Repository lane: `DynamicIiopInvocationCodecTest`
routes descriptor-backed dynamic requests and replies over loopback IIOP,
including object-reference Any values and user-exception diagnostics, while
`StaticInterfaceRepositoryTest` verifies bounded repository lookup and recursive
reference diagnostics. G10-120 records the approved live peer evidence through
the non-optional `object-reference`, `giop`, `iiop`, and `rmi-iiop` scenarios.

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

## G10-120 live scenario groups

- `basic-idl`: basic peer liveness through the approved IDL fixture;
- `object-reference`: IOR and object-reference reachability;
- `naming`: Naming URL and Naming Service fixture reachability;
- `giop`: request/reply framing through the approved GIOP lane;
- `iiop`: network IIOP request/reply reachability;
- `rmi-iiop`: Calculator IDL wire lane, including Java peer and ACE/TAO
  Calculator behavior.

Broader POA policy, Any/TypeCode, valuetype, fragmentation, and Portable
Interceptor coverage remains local verification evidence unless a future
roadmap task promotes a dedicated live scenario. Optional services require
service-specific human gates before any scenario, report fields, cache
requirements, or live execution prerequisites are added.

## G8 Trading Service dry-run lane

G8-480 adds the metadata-only `trading-service` scenario for the implemented
TRADE-10 subset. Approved peer manifests mount
`interop/idl/trading-service.idl`, dry runs enumerate both peer directions for
JVM and Native Image local lanes, and `--require-live` writes structured
missing-prerequisite reports for absent approval, IDL, local commands, native
binaries, cache, digest-pinned base image, container runtime, prepared peer
image, and unapproved live execution. It does not start peer containers or local
live lanes, and it records no live peer pass/fail evidence. G8-490 closes that
Trading Service dry-run interop posture as part of the local/IIOP/Native
Image/dry-run conformance record.

## G8 Transaction Service dry-run lane

G8-580 adds the metadata-only `transaction-service` scenario for the
implemented TRANS-14 subset. Approved peer manifests mount
`interop/idl/transaction-service.idl`, dry runs enumerate both peer directions
for JVM and Native Image local lanes, and `--require-live` writes structured
missing-prerequisite reports for absent approval, IDL, local commands, native
binaries, cache, digest-pinned base image, container runtime, prepared peer
image, and unapproved live execution. It does not start peer containers or local
live lanes, and it records no live peer pass/fail evidence. G8-590 closes that
Transaction Service dry-run interop posture as part of the local/IIOP request
context/Native Image/dry-run conformance record.

G10-080 records deterministic local Portable Interceptor evidence for the
implemented ORB/IIOP loopback path: client/server request-flow ordering,
service-context propagation, and callback failure diagnostics. Live peer
execution for the approved peer-facing request/reply lanes is included in the
G10-120 full matrix.

G10-090 records deterministic local RMI-IIOP compatibility evidence: explicit
Java-to-IDL models and generated bindings preserve
remote-interface inheritance, bounded CDR payloads carry sequences, remote
object-reference keys, declared-value members, and user-exception payload
fields, and the existing loopback wire path returns those payloads without Java
serialization, classpath scanning, or reflection metadata. G10-120 adds live
`rmi-iiop` peer pass/fail evidence across all approved peers and JVM/native
local lanes.

G10-100 adds deterministic local Native Image evidence before live peer
execution. The native-image smoke suite builds and runs aggregate
`interopClient` and `interopServer` binaries for the completed local G10 lanes
when GraalVM Native Image is available. The interop CLI also records structured
native-lane `infrastructure-failure` reports when required native client or
server binaries are missing. G10-120 includes Native Image local client and
server lanes in the full approved live matrix.

G10-110 closes the real-peer harness prerequisite for live execution. The
harness validates approved external cache entries, digest-pinned base-image
inputs, Docker/Podman availability, prepared peer images, real peer command
entrypoints, mounted scenario IDL, IOR/log/report directories, detached server
lifecycle, health checks, cleanup, and clean-room failure classification before
running JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, or ACE/TAO as
black-box peers.

## G12-050 broad IDL corpus lane

G12-050 adds local-only broad IDL fixture scenarios under `interop/idl/g12-wide/`:

- `g12-wide-core-types` maps to `CoreTypes.idl` and covers typedefs, bounded
  sequences, enums, structs, unions, exceptions, attributes, holder-using
  operation parameters, raises clauses, and context clauses.
- `g12-wide-repository-pragmas` maps to `RepositoryPragmas.idl` and covers
  repository prefix/typeprefix metadata, native declarations, value boxes,
  abstract valuetype bases, supported interfaces, and factories.
- `g12-wide-valuetypes` maps to `ValueTypes.idl` and covers native handles,
  abstract interfaces with operations, valuetype inheritance, state fields,
  factories, and supported interface operation placeholders.
- `g12-wide-unsupported-custom-value` maps to `UnsupportedCustomValue.idl` and is
  parser/semantic-valid but intentionally rejected by the IDL-to-Java mapping
  until custom value marshaling is implemented.

The G12-050 evidence is deterministic and local: `corba-codegen` tests parse,
analyze, map, generate descriptors/sources, and compile the supported fixtures;
`interop-peer local-lane-report` writes local JVM structured reports for selected
fixtures; `native-lane-report` records Native Image missing-prerequisite reports
for the same scenario names. These scenarios are not added to peer manifests in
G12-050, so no live peer compatibility is claimed by this task.

## G12-060 broad IDL peer scenario

G12-060 promotes `g12-wide-core-types` to the approved peer manifest scenario
set for JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, and ACE/TAO.
Each peer manifest records `scenarioCapabilities` metadata for the selected
fixture: the IDL path, mounted-IDL/object-reference smoke support level, the
peer-server/local-client and local-server/peer-client directions, JVM and Native
Image local runtimes, and expected structured classifications.

The harness filters `all` peer targets by declared scenario support and still
rejects an explicitly requested peer/scenario pair when that peer does not
declare the scenario. Missing cache, base image, runtime, peer image, local JVM
command, or Native Image binary inputs remain structured `infrastructure-failure`
reports. Summary aggregation includes G12 reports but raw live reports, logs,
IORs, peer artifacts, Docker layers, and native binaries remain ignored local
outputs and must not be committed.

The selected G12 peer scenario is not a valuetype marshaling claim. The
`g12-wide-repository-pragmas`, `g12-wide-valuetypes`, and
`g12-wide-unsupported-custom-value` fixtures stay local-only until later tasks
approve value marshaling behavior and peer-specific fixture adapters.

## G12-130 persistent IOR local lane

G12-130 adds deterministic local evidence for persistent IOR preservation.
Durable POA references emit opaque `MJOK` object-key octets into IIOP profiles,
and binary IORs, stringified IORs, KeyAddr, ProfileAddr, and ReferenceAddr
target addressing preserve those bytes instead of reinterpreting them as text.

The local loopback restart scenario recreates the same durable ORB identity,
POA path, object id, servant binding, and endpoint, then dispatches through the
previously stringified IOR. Malformed durable-key prefixes fail with structured
diagnostics before invocation, while valid but unbound durable keys remain
unknown-object failures. The structured report evidence is local-only under the
`g12-persistent-ior-roundtrip` scenario; G12-130 does not promote a new live
peer scenario or commit raw report outputs.

## G13-010 cross-process durable restart lane

G13-010 upgrades the local persistent IOR and persistent Naming evidence from
same-process restart simulation to forked JVM restart tests. The IIOP lane
records a stringified persistent IOR from the first server process, exits that
process, starts a second server process with the same durable ORB id, endpoint,
POA path, object id, and binding, and dispatches through the old IOR. Wrong-ORB
and missing-binding restarts fail as deterministic unknown-object cases.

The Naming lane records a remote `corbaname` value from the first persistent
Naming server process, exits it, starts a second process with the same durable
ORB id, endpoint, and `MJNS` store, and resolves the old `corbaname` value.
This is still local loopback evidence only; it does not approve live peer
persistent IOR or Naming execution.

## G13-050 durable peer persistence design

G13-050 defined future live peer scenarios for durable IOR and persistent
Naming behavior, but did not approve live execution. G14-020 promotes that
design into dry-run peer manifest metadata without approving live execution.
The peer set is the existing approved G6-820 set: JacORB,
Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, and ACE/TAO. The peer claim is
only opaque object-key preservation. Peers are not expected to parse or
understand the project-owned `MJOK` durable object-key format or the `MJNS`
Naming store format.

The proposed scenario names are:

- `g13-durable-ior-peer-client-restart`: a peer client receives a stringified
  persistent IOR emitted by our first server process, that process exits, our
  second server process starts with the same durable ORB id, endpoint, POA
  path, object id, and operation fixture, and the peer client invokes through
  the old IOR.
- `g13-durable-naming-peer-client-restart`: a peer client receives a
  `corbaname` value or persistent Naming Service IOR from our first Naming
  server process, that process exits, our second Naming server starts with the
  same durable ORB id, endpoint, and `MJNS` store, and the peer client resolves
  the old value and invokes the resolved durable target where the peer supports
  that object-reference flow.

The proposed direction matrix is intentionally one-sided:

| Scenario | Direction | Local server runtime | Peer role | Claim |
|---|---|---|---|---|
| `g13-durable-ior-peer-client-restart` | `local-server-to-peer-client` | JVM | client | Peer preserves opaque durable object-key octets from old IOR into the restarted server request. |
| `g13-durable-ior-peer-client-restart` | `local-server-to-peer-client` | Native Image | client | Same as JVM lane, using the Native Image server binary once a later task approves and wires it. |
| `g13-durable-naming-peer-client-restart` | `local-server-to-peer-client` | JVM | client | Peer preserves the persistent Naming IOR/corbaname path and resolves against the restarted Naming service. |
| `g13-durable-naming-peer-client-restart` | `local-server-to-peer-client` | Native Image | client | Same as JVM lane, using the Native Image Naming server binary once a later task approves and wires it. |

`peer-server-to-local-client` directions are out of scope for these scenarios
because a peer server does not emit mundane Java ORB durable keys. G14-020
adds only the dry-run manifest metadata for these local-server-to-peer-client
directions; live execution remains gated until deterministic
missing-prerequisite reports and maintainer live approval are both recorded.

G14-030 adds deterministic missing-prerequisite reports for this durable lane.
The cache and image prerequisites match the existing live matrix: approved
external peer cache entries, digest-pinned Java and native base images, prepared
peer images, Docker or Podman, local JVM lane commands, Native Image server
binaries when native lanes are selected, and an explicit container network.
Missing cache entries, missing images, missing local lane commands, missing
Native Image binaries, startup timeouts, early server exits, and unsupported
peer scenario metadata must become structured reports instead of skipped or
implicit results.

The expected durable-specific report fields are scenario, peer, peer version,
direction, local server runtime, peer client runtime, durable ORB id label,
POA path label, object id label, first-generation endpoint, restarted endpoint,
restart phase, stringified IOR path, corbaname value when applicable, Naming
store label when applicable, status, classification, exit code, report path,
stdout path, stderr path, start/end timestamps, and clean-room notes. Reports
must not include raw `MJOK` decoded internals beyond non-secret fixture labels,
raw `MJNS` store bytes, peer artifacts, Docker layers, Native Image binaries,
or copied reference implementation material.

Expected classifications include `durable-ior-invoked`,
`durable-naming-resolved`, `server-ready`, `expected-deferral`,
`unsupported-scenario`, `missing-prerequisite`, `infrastructure-failure`,
`our-bug`, `peer-bug`, `profile-mismatch`, and `spec-ambiguity`. G14-040 ran
the approved live matrix on 2026-05-31. Raw reports, logs, IORs, Naming stores,
peer artifacts, Docker layers, and native binaries remain ignored local outputs.

## G14-040 durable peer live evidence

G14-040 executed the approved one-sided durable persistence matrix against
JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, and ACE/TAO using the
ignored approved artifact cache, local Docker peer images, the JVM server lane,
and the SDKMAN GraalVM Native Image server lane.

Durable IOR evidence passed for all four peers and both local runtimes. Each
peer client invoked an old stringified IOR after the first local server process
exited and the second process restarted with the same fixture durable ORB id,
POA path, object id, and endpoint. The accepted classification is
`durable-ior-invoked` for all eight peer/runtime cells.

Persistent Naming evidence passed for JacORB, Eclipse GlassFish CORBA ORB, and
JBoss OpenJDK ORB across both local runtimes. Each peer client resolved an old
corbaname value after the first local Naming process exited and the second
process restarted with the same durable ORB id, Naming endpoint, durable target
endpoint, and ignored `MJNS` store. The accepted classification is
`durable-naming-resolved` for those six cells.

ACE/TAO preserved opaque durable IOR object keys, but its generated CosNaming
client path reported `MARSHAL` while resolving the persistent Naming corbaname
against the current bounded Naming service profile. That result is recorded as
`profile-mismatch` for the JVM and Native Image local Naming lanes. It is not an
opaque object-key preservation failure and does not require ACE/TAO to
understand `MJOK` or `MJNS`.

| Scenario | Peer | JVM local server | Native Image local server |
|---|---|---|---|
| durable IOR restart | JacORB | `durable-ior-invoked` | `durable-ior-invoked` |
| durable IOR restart | GlassFish CORBA ORB | `durable-ior-invoked` | `durable-ior-invoked` |
| durable IOR restart | JBoss OpenJDK ORB | `durable-ior-invoked` | `durable-ior-invoked` |
| durable IOR restart | ACE/TAO | `durable-ior-invoked` | `durable-ior-invoked` |
| durable Naming restart | JacORB | `durable-naming-resolved` | `durable-naming-resolved` |
| durable Naming restart | GlassFish CORBA ORB | `durable-naming-resolved` | `durable-naming-resolved` |
| durable Naming restart | JBoss OpenJDK ORB | `durable-naming-resolved` | `durable-naming-resolved` |
| durable Naming restart | ACE/TAO | `profile-mismatch` | `profile-mismatch` |

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

The remaining G10-120 work is tracked as child tasks. G10-120-020 added local
JVM/native direction commands and passed the approved ACE/TAO `basic-idl`
direction matrix with an explicit Docker network, host-gateway routing, and
rebuilt Native Image lane binaries. G10-120-030 made peer commands
scenario-aware and passed ACE/TAO `rmi-iiop` Calculator execution in both
`run-scenario` and JVM/native direction-matrix form after rebuilding the
approved ACE/TAO image and Native Image lane binaries. That live run also
confirmed GIOP operation-body alignment and BOM-prefixed CDR `wstring`
compatibility with TAO-generated stubs/skeletons. G10-120-040 completed the
Java peer matrix bootstrap for ACE/TAO, JacORB, and GlassFish non-RMI smoke
lanes. G10-120-050 fixed JBoss OpenJDK ORB `basic-idl` readiness by avoiding a
duplicate Java ORB listener configuration. G10-120-060 runs Java peer
`rmi-iiop` direction matrices and classifies the remaining Java RMI-IIOP
failures as project-owned `our-bug` evidence. G10-120-080 closes those defects
with peer-visible code-set metadata, request/reply code-set service contexts,
flexible Java ORB `wstring` decoding, corrected Java peer empty-exception
marshaling, and fresh Native Image lane binaries. The JacORB, GlassFish CORBA
ORB, and JBoss OpenJDK ORB `rmi-iiop` direction matrices now pass for both JVM
and Native Image local lanes with zero remaining `our-bug` classifications.
G10-120-090 records final live direction-matrix evidence across every approved
non-optional scenario. On 2026-05-26, `run-direction-matrix --require-live
<scenario> all` passed for `basic-idl`, `object-reference`, `naming`, `giop`,
`iiop`, and `rmi-iiop` using the approved cache, digest-pinned base images,
Docker, SDKMAN GraalVM Native Image binaries, JVM/native local lane commands,
and the explicit `mjo-interop` Docker network. The clean-room report scan
covered 210 structured reports, 35 per scenario, all `passed`, with
classification counts of `expected-deferral` 192, `object-reference-checked`
10, `server-ready` 6, and `calculator-checked` 2. `expected-deferral` is a
passed harness classification for smoke-style peer commands whose deeper
behavior is represented by the paired direction reports, not a skip or failure.
The final evidence has zero `our-bug`, unresolved `infrastructure-failure`,
`unsupported-scenario`, or skipped classifications. Raw reports, logs, IORs,
peer artifacts, Docker layers, and native binaries remain ignored local output
and are not committed.

## Optional service lanes

G8 accepts staged designs for Time, Event, Notification, Trading, Transaction,
and Security/CSIv2 Services through ADR-0016 through ADR-0022. G8-100
implements the local Time Service value and clock-query slice. G8-110 adds
local loopback IIOP/Naming exposure for `universal_time`,
`new_universal_time`, and `new_interval` using explicit TimeBase field codecs.
G8-120 adds the `time-service` scenario metadata, approved-peer dry-run matrix,
and deterministic missing-prerequisite reports for JVM and Native Image local
lanes. G8-130 records 2026-06-06 maintainer approval for live Time Service peer
execution. G8-140 records the approved live matrix: JacORB, GlassFish CORBA
ORB, and JBoss OpenJDK ORB peer clients invoke our JVM and Native Image Time
Service servers successfully for `universal_time`, `new_universal_time`, and
`new_interval`; ACE/TAO and reverse peer-server directions are recorded as
`unsupported-scenario` for this value-returning subset. G8-240 adds local
loopback IIOP/Naming evidence for EventChannel admin lookup, proxy creation,
push, pull, try_pull, disconnect operations, primitive Any payloads, malformed
request diagnostics, and Naming-resolved EventChannel IORs. G8-270 closes the
Event Service local/IIOP/Native Image/dry-run conformance record for that
subset. The `event-service` peer metadata, `interop/idl/event-service.idl`,
JVM/native dry-run direction enumeration, and structured missing-prerequisite
reports remain metadata-only; Event Service does not have live peer lanes or
pass/fail peer evidence. G8-300 splits Notification Service into staged local,
Native Image, and metadata-only interop slices. G8-360 adds local loopback
IIOP/Naming evidence for NotificationChannel admin lookup, structured proxy
creation, structured push/pull/try-pull operations, filter/QoS rejection
diagnostics, malformed request diagnostics, and Naming-resolved
NotificationChannel IORs without peer execution. G8-380 adds the metadata-only
`notification-service` scenario, `interop/idl/notification-service.idl`,
approved-peer manifest declarations, JVM/native dry-run direction enumeration,
and structured missing-prerequisite reports for live approval, scenario IDL,
local commands/binaries, artifact cache, digest-pinned base image, container
runtime, peer image, and unapproved live execution. Notification Service has no
live peer lanes or pass/fail peer evidence. G8-390 closes that
local/IIOP/Native Image/dry-run conformance record without approving live peer
execution. G8-480 adds the metadata-only `trading-service` scenario,
`interop/idl/trading-service.idl`, approved peer manifest declarations,
JVM/native dry-run direction enumeration, and deterministic
missing-prerequisite reports before any peer execution is required. Trading
Service has no live peer lanes or pass/fail peer evidence. G8-580 adds the
metadata-only `transaction-service` scenario,
`interop/idl/transaction-service.idl`, approved peer manifest declarations,
JVM/native dry-run direction enumeration, and deterministic
missing-prerequisite reports. Transaction Service has no live peer lanes or
pass/fail peer evidence, and G8-590 closes its local/IIOP request
context/Native Image/dry-run conformance record. G8-600 splits Security/CSIv2
into staged implementation slices; `security-service` interop metadata remains
blocked until G8-680 and no live secure peer lanes or pass/fail peer evidence
are approved.
