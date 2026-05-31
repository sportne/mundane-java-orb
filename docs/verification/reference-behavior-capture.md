# Reference Behavior Capture

Reference behavior must be captured through black-box execution wherever possible.

## Approval-to-execution flow

G6-820 approves only artifact and license gates. Each selected peer has a
source-controlled approval record under `interop/approvals/` that records the
reviewer, review date, approval evidence identifier, artifact origin, license
status, clean-room restrictions, external cache path, and SHA-256. Before real
execution, run `interop/bin/interop-peer validate-gates --require-cache` with an
absolute `INTEROP_ARTIFACT_CACHE`.

G6-830 real-run commands are environment-gated. They may run approved
Docker/Podman peer containers only when the external cache and configured images
are present; otherwise they write structured infrastructure-failure reports.
The approved use is black-box interoperability through logs, IORs, wire
captures, and structured reports. Source copying, implementation
transliteration, and vendored peer source or binaries remain prohibited.

G7-090 extends this flow to the `rmi-iiop` scenario. Default validation remains
dry-run and missing-prerequisite safe; live RMI-IIOP peer reports require the
approved external cache, digest-pinned base images, and container runtime. A
missing prerequisite must produce a structured `infrastructure-failure` report
instead of an implicit skip.

G7-100 closes the local RMI-IIOP report evidence by validating that dry-run
execution does not mutate outputs, missing prerequisites produce structured
`rmi-iiop` reports, and summaries include captured RMI-IIOP report paths.

G10-110 closes the black-box harness boundary used by `G10-120`. Real peer
commands are allowed only after approval records, external cache checks,
digest-pinned base-image inputs, prepared peer images, Docker/Podman, scenario
IDL mounts, and report directories validate. The harness writes deterministic
`infrastructure-failure` reports for missing prerequisites and failed peer
commands, requires prepared images to expose real peer command scripts or
explicit command environment overrides, and never commits peer source, peer
binaries, or live outputs.

The first 2026-05-24 G10-120 prerequisite attempt confirmed that missing
`INTEROP_ARTIFACT_CACHE` and missing approved cache entries are classified as
structured `infrastructure-failure` reports before live peer behavior starts.
A later approved-cache attempt confirmed cache validation, Native Image binary
smoke execution, and limited `basic-idl` peer-command smoke success for JacORB,
JBoss OpenJDK ORB, and Eclipse GlassFish CORBA ORB, with generated reports
under `build/interop/*/reports/`. That later result is still release-blocking
prerequisite evidence, not compatibility evidence. ACE/TAO now has tracked
peer-specific image and clean-room command sources. On 2026-05-25, the approved
ACE/TAO follow-up built the local image from the approved cache archive and ran
the `basic-idl` peer smoke through server, health, client, and summary commands
on an explicit local Docker network. That result remains prerequisite evidence;
the original scenario runner does not execute the required our-JVM/our-native
versus peer client/server directions.

After maintainer approval on 2026-05-24, `run-direction-matrix` became the
structured prerequisite check for those directions. It starts peer servers
before local client lanes, starts local server lanes before peer clients, and
records missing JVM lane commands, missing Native Image binaries, missing peer
images, early local-server exits, and peer command failures as
`infrastructure-failure` reports instead of treating them as compatibility
results.

## Capture fields

```json
{
  "peer": "jacorb",
  "peerVersion": "3.9",
  "scenario": "basic-idl",
  "idl": "interop/idl/basic/BasicTypes.idl",
  "clientRuntime": "our-jvm-jdk21",
  "serverRuntime": "peer-jvm",
  "role": "server",
  "image": "corba-interop-peer-jacorb:3.9",
  "command": "server",
  "status": "passed",
  "classification": "expected-deferral",
  "exitCode": 0,
  "stdoutPath": "build/interop/jacorb/logs/basic-idl-server.stdout.log",
  "stderrPath": "build/interop/jacorb/logs/basic-idl-server.stderr.log",
  "reportPath": "build/interop/jacorb/reports/basic-idl-server.json",
  "startedAt": "2026-05-18T00:00:00Z",
  "endedAt": "2026-05-18T00:00:01Z",
  "notes": "G10-110 container command completed"
}
```

For the G7-090 RMI-IIOP lane, `scenario` is `rmi-iiop` and `idl` is
`interop/idl/rmi-iiop/Calculator.idl`.

G10-120 completion is split into child tasks so the reference captures can be
reviewed incrementally. Local JVM/native lane commands now pass the
approved ACE/TAO `basic-idl` direction matrix. Scenario-aware command closure
now also passes ACE/TAO `rmi-iiop` Calculator execution through TAO-generated
C++ stubs/skeletons and the local JVM/native direction matrix. Java peer
`rmi-iiop` direction-matrix failures are now captured as structured
project-owned `our-bug` evidence rather than unresolved infrastructure
failures. G10-120-080 closes those code-set and `wstring` compatibility defects
and reruns JacORB, GlassFish CORBA ORB, and JBoss OpenJDK ORB `rmi-iiop`
direction matrices successfully for both JVM and Native Image local lanes.
G10-120-070 adds deterministic summary counts and compact failure entries for
clean-room review without committing raw logs or IORs.

Final G10-120 reference capture completed on 2026-05-26. The approved live
matrix ran six scenarios, four peers, peer-server/local-client and
local-server/peer-client directions, and JVM plus Native Image local lanes. The
clean-room summary counted 210 structured reports, all `passed`, with
classification counts of `expected-deferral` 192, `object-reference-checked`
10, `server-ready` 6, and `calculator-checked` 2. `expected-deferral` is a
passed harness classification for smoke-style peer commands whose deeper
behavior is represented by the paired direction reports, not a skip or failure.
No `our-bug`, unresolved `infrastructure-failure`, `unsupported-scenario`, or
skipped classification remains. The committed evidence is this summary only;
raw live reports, logs, IORs, peer artifacts, Docker layers, and native binaries
remain ignored local output.

G12-060 adds the selected `g12-wide-core-types` broad-IDL fixture to approved
peer scenario metadata. Reference capture for this fixture uses the existing
black-box peer container harness, mounted source-controlled IDL, capability
filtering, structured missing-prerequisite reports, and summary aggregation.
The committed evidence remains metadata and deterministic report-schema tests;
raw G12 live outputs are ignored local artifacts.

G13-050 defined a design-only durable persistence reference-capture package for
future live peer work, and G14-020 promotes those scenario names into dry-run
peer manifest metadata. The scenarios are
`g13-durable-ior-peer-client-restart` and
`g13-durable-naming-peer-client-restart`, both limited to peer clients invoking
or resolving against our restarted JVM or Native Image servers. The reference
claim is opaque object-key preservation through a peer ORB, not peer knowledge
of `MJOK` or `MJNS`.

G14-030 durable peer reports extend the normal capture fields with:

```json
{
  "scenario": "g13-durable-ior-peer-client-restart",
  "direction": "local-server-to-peer-client",
  "localServerRuntime": "our-jvm-jdk21",
  "peerClientRuntime": "peer-jvm",
  "durableOrbIdLabel": "g13-peer-restart-orb",
  "poaPathLabel": "RootPOA/g13/persistent",
  "objectIdLabel": "fixture-object",
  "firstEndpoint": "127.0.0.1:2809",
  "restartedEndpoint": "127.0.0.1:2809",
  "restartPhase": "after-second-server-ready",
  "stringifiedIorPath": "build/interop/<peer>/iors/g13-durable.ior",
  "corbaname": "",
  "namingStoreLabel": "",
  "status": "passed",
  "classification": "durable-ior-invoked"
}
```

The Naming scenario uses `classification: durable-naming-resolved` when the peer
client resolves the old persistent Naming reference after restart. If a peer
cannot express the scenario without project-specific glue, the report must use
`unsupported-scenario`; missing cache entries, images, local lane commands,
Native Image binaries, or container runtimes must use
`infrastructure-failure` or `missing-prerequisite`. Peer behavior that drops or
rewrites opaque object-key octets is reviewed as `peer-bug`,
`profile-mismatch`, or `spec-ambiguity`; mundane Java ORB restart, Naming, or
IOR handling defects remain `our-bug`.

`status` is one of `passed`, `failed`, or `skipped`. `classification` is one of
`our-bug`, `peer-bug`, `spec-ambiguity`, `profile-mismatch`,
`infrastructure-failure`, `missing-prerequisite`, `unsupported-scenario`, or
`expected-deferral`. Approved scenario-specific success classifications also
include `object-reference-checked`, `server-ready`, `calculator-checked`,
`durable-ior-invoked`, and `durable-naming-resolved`.

## Clean-room rule

Behavioral observations can become tests. Reference implementation source code
must not become implementation code.
