# Pre-1.0 Interoperability Plan

The pre-1.0 interoperability program is tracked by the G10 roadmap tasks. It is
not a release approval by itself. It defines the non-optional implementation and
verification work that must complete before
`G10-120-PRE-1.0-FULL-INTEROP-EXECUTION` can run.

## Release Bar

Version `1.0.0` must not be declared until the non-optional CORBA, IDL,
IDL-to-Java, RMI-IIOP, Native Image, and live peer interoperability tasks in
G10 are complete. Optional CORBA Services remain deferred behind the existing G8
human gates and are not part of the 1.0.0 interop release bar.

## Task Sequence

G10 is ordered to close prerequisites before live peer execution:

1. IDL 4.2 grammar closure.
2. IDL-to-Java legacy mapping closure.
3. OMG compatibility API surface.
4. CDR, GIOP, IIOP, and IOR wire closure.
5. Network ORB/POA dispatch.
6. Network Naming and object URL behavior.
7. DynamicAny, DII, DSI, Any, TypeCode, and Interface Repository wire closure.
8. Portable Interceptors.
9. RMI-IIOP compatibility closure.
10. Native Image interop binaries.
11. Real peer harness closure.
12. Full pre-1.0 live interop execution.

All prerequisite G10 tasks are complete. G10-110 completed the prerequisite
harness closure, and G10-120 completed the approved full live matrix on
2026-05-26.

## Peer And Scenario Policy

Approved peers remain JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB,
and ACE/TAO. Peer use stays black-box only: no vendored peer source, committed
peer binaries, source-derived implementation logic, or committed live outputs.

The G10-120 live matrix covers the approved non-optional peer scenarios:
`basic-idl`, `object-reference`, `naming`, `giop`, `iiop`, and `rmi-iiop`.
Each scenario runs peer-server/local-client and local-server/peer-client
directions with JVM and Native Image local lanes where applicable. Broader
DynamicAny/DII/DSI, Portable Interceptor, TypeCode, POA-policy, valuetype, and
fragmentation coverage remains local verification evidence unless a future
roadmap task promotes a dedicated live scenario. Optional services are excluded
until service-specific human gates approve separate scenarios.

## Execution Prerequisites

Live interop execution requires:

- approved external peer cache entries verified by SHA-256;
- digest-pinned Java and native base images;
- Docker or Podman;
- Native Image when native lanes are required;
- structured reports for pass, fail, skipped, and infrastructure outcomes.

Cache preparation must be opt-in and write only to an untracked external cache.
No live fetch is part of the default local validation path.

The harness validates cache and image prerequisites before live execution and
writes structured `infrastructure-failure` reports for missing cache entries,
missing digest-pinned base images, missing container runtimes, missing prepared
peer images, missing real peer commands, failed health checks, or failed peer
commands. Successful peer commands become a 1.0.0 compatibility claim only
when they are part of the recorded G10-120 clean-room evidence.

## Current G10-120 Attempts

On 2026-05-24, local execution stopped before live peer behavior because
`INTEROP_ARTIFACT_CACHE` was unset. The required
`interop/bin/interop-peer validate-gates --require-cache` command failed with
missing approved cache entries for ACE/TAO, Eclipse GlassFish CORBA ORB,
JacORB, and JBoss OpenJDK ORB. A `basic-idl` live run attempt wrote structured
server-lane `infrastructure-failure` reports for all four peers at prerequisite
validation. These reports are not committed live evidence and do not satisfy the
1.0.0 release bar.

A follow-up attempt used the approved repo-local ignored cache at
`/mnt/d/projects/mundane-java-orb/interop/work/artifact-cache`, digest-pinned
Java and native base images, Docker, and GraalVM Native Image from SDKMAN.
Cache validation and Native Image binary smoke execution passed. Ignored local
peer-image smoke runs reached `basic-idl` peer-command success for JacORB,
JBoss OpenJDK ORB, and Eclipse GlassFish CORBA ORB after the approved cache
entries were expanded and GlassFish ran with GMBAL initialization deferred.
The generated, uncommitted prerequisite reports were written under
`build/interop/jacorb/reports/`, `build/interop/jboss-openjdk-orb/reports/`,
and `build/interop/glassfish-orb/reports/`.

That follow-up is still not 1.0.0 compatibility evidence. On 2026-05-25, the
approved ACE/TAO follow-up built `corba-interop-peer-ace-tao:8.0.6` from the
approved cache archive and digest-pinned native base image, using tracked
clean-room peer glue. A live `basic-idl` peer smoke then started the ACE/TAO
server, parsed the server IOR from health/client containers on an explicit
local Docker network, and wrote a structured report summary. The raw reports
remain ignored local output. The original `run-scenario` harness starts a peer
server, checks peer health, and then starts a peer client; it does not execute
the required our-JVM-client to peer-server, our-native-client to peer-server,
peer-client to our-JVM-server, or peer-client to our-native-server directions.

Maintainer approval on 2026-05-24 allowed the harness direction work needed to
make those gaps explicit. The new `run-direction-matrix` command starts each
selected peer server before running our JVM/native client lanes, and starts our
JVM/native server lanes before running the selected peer client. It passes the
scenario, peer, IDL path, IOR directory, and expected server IOR path through
environment variables so approved lane commands can participate in the matrix.
It intentionally fails with `infrastructure-failure` reports when
`MJO_JVM_CLIENT_COMMAND`, `MJO_JVM_SERVER_COMMAND`,
`MJO_NATIVE_CLIENT_BINARY`, `MJO_NATIVE_SERVER_BINARY`, prepared peer images,
or approved cache inputs are missing. G10-120-090 supplies the complete
approved live scenario execution and clean-room evidence summaries.

G10-120 is split into child tasks so each remaining blocker has a separate
review and commit boundary. G10-120-020 added local JVM/native client and server
lane commands and passed the approved ACE/TAO `basic-idl` direction matrix.
G10-120-030 closed scenario-aware peer commands including the ACE/TAO
`rmi-iiop` Calculator lane and passed the approved ACE/TAO `rmi-iiop`
direction matrix. G10-120-040 completed Java peer bootstrap for the non-RMI
smoke lanes, and G10-120-050 made JBoss OpenJDK ORB `basic-idl` server
readiness deterministic. G10-120-060 runs the Java peer `rmi-iiop` direction
matrices and now produces structured `our-bug` ownership evidence instead of
generic infrastructure failures for the remaining Java RMI-IIOP code-set and
`wstring` compatibility gaps. G10-120-080 closes those project-owned defects
and passes the JacORB, GlassFish CORBA ORB, and JBoss OpenJDK ORB `rmi-iiop`
direction matrices for both JVM and Native Image local lanes. G10-120-070 adds
compact status, classification, scenario, and failure summaries so final
clean-room evidence can be reviewed without committing raw live outputs.
G10-120-090 records final live matrix evidence. The final execution used the
approved repo-local ignored artifact cache, digest-pinned Java and native base
images, Docker, GraalVM Native Image binaries, JVM/native local lane commands,
and an explicit `mjo-interop` container network. The complete approved matrix
passed for `basic-idl`, `object-reference`, `naming`, `giop`, `iiop`, and
`rmi-iiop` across JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, and
ACE/TAO. The clean-room report scan counted 210 structured reports: 35 per
scenario, all `passed`, with classification counts of `expected-deferral` 192,
`object-reference-checked` 10, `server-ready` 6, and `calculator-checked` 2.
`expected-deferral` is a passed harness classification for smoke-style peer
commands whose deeper behavior is represented by the paired direction reports,
not a skip or failure. The final evidence has zero `our-bug`, unresolved
`infrastructure-failure`, `unsupported-scenario`, or skipped classifications.
Raw live reports, logs, IORs, peer artifacts, Docker layers, and native
binaries remain ignored local output and are not committed.

## Acceptance

`G10-120` passed because each non-optional scenario passed in the approved live
matrix. No maintainer-approved `peer-bug`, `spec-ambiguity`, or
`profile-mismatch` classification was needed, and no `our-bug` or unresolved
infrastructure failure remains in the final evidence set.

## Post-1.0 Durable Persistence Follow-Up

G13 durable peer persistence work is outside the 1.0.0 release bar. G13-050
defines future design-only scenarios for peer clients invoking old persistent
IORs and resolving old persistent Naming references after our server process
restarts. G14-020 adds dry-run peer manifest metadata for those scenarios. The
approved G10 peer set remains the candidate peer set, but no live durable IOR
or persistent Naming execution is approved by this plan.

Future execution must be human-gated and limited to opaque object-key
preservation claims. Peers are not expected to understand the project-owned
`MJOK` durable key or `MJNS` Naming store formats. Raw live durable reports,
logs, IORs, Naming stores, peer artifacts, Docker layers, and Native Image
binaries must remain ignored local outputs unless a later release evidence task
approves a clean-room summary.
