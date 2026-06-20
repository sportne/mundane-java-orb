# mundane Java ORB

This repository is a design-first starting point for mundane Java ORB, a
complete Java ORB with legacy CORBA compatibility, modern generated-code APIs,
GraalVM Native Image support, multi-ORB interoperability testing, and strong
coding-agent guardrails.

## Development model

This project is largely coding-agent driven. Human maintainers set the direction,
review the design-control documents, and approve implementation gates; coding
agents are expected to do much of the scaffold, documentation, build, and
eventually implementation work under the rules in `AGENT.md`.

That workflow is intentional, so the repository is structured to be explicit
about requirements, architecture, verification, build behavior, and roadmap
tasks. Contributor-facing documentation is part of the product, not an
afterthought.

## Current repository phase

This repository is in **post-1.0.0 durable peer persistence evidence state**:

- governance documents exist;
- requirement and ADR templates exist;
- specification traceability structure exists;
- Gradle 9.5.1 Groovy DSL build infrastructure is scaffolded;
- JUnit Platform/Jupiter, ArchUnit, JaCoCo, Checkstyle, SpotBugs, Error Prone,
  Spotless, publishing, offline-build, and Native Image hooks are scaffolded;
- shared diagnostics and bounded-limit foundation types have started;
- the IDL lexer, minimal preprocessor, parser, AST, and semantic-analysis
  foundations have started under approved roadmap tasks;
- the validation-only `corba-idlj validate` CLI has started under approved
  roadmap tasks;
- compile-safe minimal IDL-to-Java mapping and source generation have started
  under approved roadmap tasks;
- compile-safe richer IDL-to-Java mapping for native declarations, value boxes,
  valuetypes, richer descriptors, and TypeCode metadata has started under
  approved roadmap tasks;
- broad local IDL feature corpus fixtures and deterministic local JVM/Native
  Image interop reports have started under approved roadmap tasks;
- the selected broad IDL core fixture is declared as an approved peer scenario
  with capability-filtered live-report harness coverage under approved roadmap
  tasks;
- durable ORB/POA identity, persistent POA object keys, persistent IOR loopback
  round trips, and caller-configured Naming persistence are implemented as
  staged G12 runtime identity slices;
- CDR primitive and length-bearing value read/write behavior has started under
  approved roadmap tasks;
- IOR, IIOP profile body, stringified IOR, `corbaloc`, and `corbaname` value
  parsing has started under approved roadmap tasks;
- in-process local object reference invocation has started under approved
  roadmap tasks;
- deterministic local exception mapping has started under approved roadmap
  tasks;
- bounded GIOP 1.2 message read/write behavior has started under approved
  roadmap tasks;
- local loopback IIOP TCP request/reply transport has started under approved
  roadmap tasks;
- endpoint-local IIOP TLS/mTLS transport configuration has started under
  approved roadmap tasks;
- the POA policy matrix and POA-lite/full-POA implementation boundary have
  started under approved roadmap tasks;
- POA-lite active-object-map servant dispatch has started under approved
  roadmap tasks;
- local full-POA policy behavior, POA managers, default servants, servant
  managers, adapter activators, user object IDs, multiple IDs, and implicit
  activation have started under approved roadmap tasks;
- local descriptor-backed TypeCode metadata and Any payload CDR round-trips
  have started under approved roadmap tasks;
- local descriptor-backed DynamicAny, DII-style invocation, and DSI-style
  skeleton dispatch have started under approved roadmap tasks;
- local static Interface Repository metadata over generated descriptors has
  started under approved roadmap tasks;
- local in-memory CosNaming behavior and `corbaname:rir:` resolution have
  started under approved roadmap tasks;
- bounded loopback IIOP Naming Service behavior, IOR exchange, and remote
  `corbaloc`/`corbaname` resolution have started under approved roadmap tasks;
- approved peer interop gates and environment-gated clean-room report capture
  have started under approved roadmap tasks;
- centralized GraalVM Native Image smoke binaries for IDL validation,
  generated-style local invocation, naming, IOR diagnostics, and interop report
  parsing have started under approved roadmap tasks;
- offline release validation now stages publications, validates BOM alignment,
  prepares checksum-manifested Maven repositories, and builds a standalone
  downstream consumer under approved roadmap tasks;
- compatibility, security/fuzz-style hostile-input, structured interop failure,
  Native Image boundary, and bounded performance/soak closure evidence has been
  recorded under approved roadmap tasks;
- the RMI-IIOP and Java-to-IDL design gate has an accepted ADR and a G7
  implementation roadmap;
- explicit RMI Java eligibility diagnostics have started under approved roadmap
  tasks;
- in-memory Java-to-IDL mapping models for eligible RMI Java declarations have
  started under approved roadmap tasks;
- explicit metadata-based RMI repository ID construction and planning have
  started under approved roadmap tasks;
- deterministic generated IDL fixtures for the first parser-supported RMI
  Java-to-IDL slice have started under approved roadmap tasks;
- compile-safe RMI-IIOP binding source surfaces, helpers, holders, stubs, ties,
  skeleton placeholders, and generated binding descriptors have started under
  approved roadmap tasks;
- bounded local RMI-IIOP CDR value and empty user-exception payload marshaling
  for the approved binding slice has started under approved roadmap tasks;
- local RMI-IIOP generated stubs, ties, skeletons, and binding descriptors now
  invoke through in-process ORB/POA adapters under approved roadmap tasks;
- bounded local JVM RMI-IIOP wire integration over existing GIOP/IIOP
  request/reply paths has started under approved roadmap tasks;
- loopback IIOP ORB/POA dispatch for activated local servants has started under
  approved roadmap tasks, including deterministic target-address routing and
  normal/user/system reply mapping;
- Portable Interceptor request-flow callbacks and service-context propagation
  over the loopback ORB/IIOP path have started under approved roadmap tasks;
- RMI-IIOP compatibility now covers explicit sequence payloads, deterministic
  remote object-reference keys, declared-value member metadata, user-exception
  payload fields, and remote interface inheritance metadata in local generated
  binding and bounded CDR/GIOP/IIOP wire paths;
- Native Image interop smoke coverage now includes aggregate client/server
  binaries for the completed local G10 interop lanes, plus structured
  native-lane missing-prerequisite reports for absent native binaries;
- the real peer harness now validates approved external caches, digest-pinned
  base images, prepared peer images, container runtime availability, scenario
  IDL mounts, IOR/log/report paths, and clean-room failure classification before
  live G10 peer execution;
- pre-1.0 full live peer execution has completed for the approved non-optional
  matrix: `basic-idl`, `object-reference`, `naming`, `giop`, `iiop`, and
  `rmi-iiop` all passed across JacORB, JBoss OpenJDK ORB, Eclipse GlassFish
  CORBA ORB, and ACE/TAO for JVM and Native Image local lanes;
- `corba.version` is set to `1.0.0`, and the release workflow packages the
  locally staged Maven repository as GitHub Release assets only;
- the approved non-human-gated G12 durable identity implementation tasks are
  complete through caller-configured Naming persistence;
- RMI-IIOP peer interop scenario gates, structured report paths, and approved
  live peer direction-matrix evidence have completed under G10;
- RMI-IIOP Native Image smoke coverage, source-level Native Image metadata
  audits, hostile-input closure, and structured interop-report closure have
  started under approved roadmap tasks;
- optional Time, Event, Notification, and Trading Service implementation
  subsets now have closed G8 conformance records covering local behavior,
  loopback IIOP/Naming, Native Image smoke, and approved dry-run peer metadata;
  Transaction Service / OTS has started with a bounded local
  coordinator/resource model, and Security/CSIv2 remains blocked behind its
  accepted G8 ADR design;
- the G10 pre-1.0 interoperability roadmap has closed non-optional IDL,
  mapping, wire, runtime, Native Image, harness, and live peer execution gaps
  needed before `1.0.0` release publication;
- the G12 post-1.0 roadmap starts with IDL compiler hardening, then broad IDL
  feature corpus interop, then live peer scenarios for those richer fixtures,
  before later durable runtime identity design work;
- the G13 durable runtime hardening roadmap starts with local cross-process
  durable IOR and persistent Naming restart evidence before operational Naming
  store hardening, `MJNS` versioning policy, durable POA rehydration design, or
  live peer persistence claims;
- the G14 durable peer persistence roadmap has accepted local durable routing
  evidence and is ready for the scoped live durable IOR/Naming execution; any
  broader durable peer claim remains gated by a separate maintainer decision;
- CORBA runtime, ORB, POA, service, and generated-code behavior remain limited
  to approved roadmap tasks.

## Primary goals

The project shall eventually provide independently usable artifacts for:

- `org.omg.*` legacy API compatibility;
- modern Java generated-code APIs;
- an `idlj`-like compiler;
- CDR, IOR, GIOP, IIOP, ORB, POA, Any, TypeCode, DynamicAny, DII, DSI;
- CosNaming and optional CORBA services;
- RMI-IIOP / Java-to-IDL where required;
- JVM and GraalVM Native Image execution;
- interoperability with JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB,
  and ACE/TAO.

## Normative baseline

- Primary CORBA target: CORBA 3.4.
- Compatibility profiles: CORBA 3.3, CORBA 3.2, legacy Java/CORBA behavior,
  and ACE/TAO C++ interoperability.
- IDL target: OMG IDL 4.2.
- Legacy Java mapping target: OMG IDL to Java Language Mapping.

See `docs/standards-baseline.md` and `docs/compatibility-profiles.md`.

## Agent rule

No implementation task may begin without:

1. requirement IDs;
2. specification references;
3. target module;
4. allowed and forbidden files;
5. required tests;
6. required documentation updates;
7. an exact acceptance command.

See `AGENT.md` and `docs/roadmap/implementation-task-template.md`.

## Build status of this scaffold

This repository includes Gradle build files and wrapper metadata targeting Gradle
9.5.1. In a normal development environment, run:

```bash
./gradlew projects
./gradlew validateDesignControlPack
./gradlew qualityGate
./gradlew offlineReleaseValidation
./gradlew printPublishedArtifacts
```

For build setup details, see `docs/build/README.md`. For offline environments,
see `docs/build/offline-build.md` and `tools/prepare-offline-repository.sh`.
For release publication details, see `docs/verification/github-release-assets.md`
and `docs/releases/1.0.0.md`.

Build conventions live in `build-logic/` as composable Gradle convention plugins. Published and internal test modules live under `modules/`; non-published examples live under `examples/`.

## Ready roadmap tasks

G8 optional-service design gates are complete, and the Time Service local
value/clock, loopback IIOP/Naming exposure, structured interop metadata, and
approved live peer conformance closure slices are implemented.
Event Service, Notification Service, and Trading Service local/IIOP/Native
Image/dry-run conformance records are also closed. Trading Service covers
bounded local type and offer repositories, constraint evaluation, local query,
import/export boundary metadata, loopback IIOP/Naming exposure, Native Image
smoke, and dry-run interop metadata without live peer claims. Transaction
Service / OTS has started with a bounded local coordinator/resource model;
Security/CSIv2 remains blocked.
G13-000 through G13-090 and G14-000 through G14-040 are complete. G14 live
durable peer evidence covers old durable IOR invocation across JacORB,
GlassFish CORBA ORB, JBoss OpenJDK ORB, and ACE/TAO for JVM and Native Image
local servers.
Persistent Naming restart evidence resolves for JacORB, GlassFish CORBA ORB,
and JBoss OpenJDK ORB across JVM and Native Image local servers; ACE/TAO is
recorded as a durable Naming `profile-mismatch`, not as an opaque-key failure.
Time Service live peer evidence covers peer clients invoking our JVM and Native
Image Time Service servers for `universal_time`, `new_universal_time`, and
`new_interval` across JacORB, GlassFish CORBA ORB, and JBoss OpenJDK ORB.
ACE/TAO and reverse peer-server Time Service lanes are recorded as
`unsupported-scenario` for this value-returning subset, not as compatibility
claims.

Future live peer expansion beyond the completed G14 durable IOR/Naming
directions stays out of scope until maintainers record a separate decision.
Event Service now has a local channel model with bounded options, stable
diagnostics, supplier/consumer admin surfaces, proxy handles, and in-JVM push
and pull delivery, including bounded fan-out/backpressure diagnostics.
Descriptor-backed loopback IIOP/Naming exposure now covers channel admin
lookup, proxy creation, push, pull, try_pull, disconnect operations, malformed
request diagnostics, and Naming-resolved EventChannel IORs. Native Image smoke
coverage now exercises local channel creation, push and pull delivery, bounded
rejection, loopback IIOP/Naming exposure, and clean shutdown for that subset.
Event Service interop metadata declares the `event-service` scenario for
approved peers, mounts `interop/idl/event-service.idl`, enumerates JVM/native
dry-run directions, and writes deterministic missing-prerequisite reports
without live peer claims. G8-270 closes the local/IIOP/Native Image/dry-run
Event Service conformance record for that subset. G8-300 splits Notification
Service into narrow implementation slices. G8-310 adds the Notification Service
local channel lifecycle and Event Service compatibility boundary, while G8-320
adds the immutable structured-event model with fixed identity fields, primitive
named properties, bounded header/body sections, and deterministic validation.
G8-330 adds bounded filter parsing/evaluation over identity fields and primitive
filter properties without delivery routing. G8-340 adds bounded QoS/admin
policy validation for channel, admin, proxy, queue, filter, durable, and
transaction policy keys. G8-350 adds in-JVM structured push/pull delivery with
bounded filters, queues, failed-consumer handling, and stale-proxy diagnostics.
G8-360 adds descriptor-backed loopback IIOP/Naming exposure for channel/admin
lookup, structured proxy creation, structured push/pull operations, local
filter/QoS rejection diagnostics, malformed request diagnostics, and
Naming-resolved NotificationChannel IORs. G8-370 adds Native Image smoke
coverage for channel creation, structured event validation, filter validation,
QoS rejection, local delivery, loopback IIOP/Naming exposure, and clean
shutdown. G8-380 adds the `notification-service` IDL fixture, approved-peer
metadata, JVM/native dry-run direction enumeration,
`InteropScenario.notificationService()`, and structured missing-prerequisite
reports without starting peer containers or claiming live peer compatibility.
G8-390 closes the Notification Service local/IIOP/Native Image/dry-run
conformance record for that subset. G8-400 splits Trading Service into narrow
implementation slices covering type repository, offer repository, bounded
constraints, local query, import/export boundary metadata, loopback IIOP/Naming,
Native Image smoke, interop metadata, and conformance closure. G8-410 adds the
bounded local type repository with primitive property definitions, immutable
snapshots, configured limits, and stable diagnostics. G8-420 adds bounded
in-memory offer CRUD over registered service types with primitive property
values, immutable snapshots, deterministic list-by-type ordering, and stable
compatibility/limit diagnostics. G8-430 adds the bounded constraint parser and
evaluator over primitive property maps with deterministic hostile-input
diagnostics. G8-440 adds bounded local type-scoped offer query with
deterministic offer-ID ordering, result/cost limits, and clear constraint
diagnostics. G8-450 adds bounded import/export link metadata with direction,
fan-out, duplicate/missing link, disabled-federation, and wrong-direction
diagnostics while preserving local-query isolation. G8-460 adds
descriptor-backed loopback IIOP/Naming exposure for the supported local Trader
facade: type registration/update/delete/lookup/list, offer registration and
withdrawal, local query, import/export metadata listing, disabled import
diagnostics, malformed request diagnostics, stale/unknown object-key and
unknown-operation diagnostics, Naming-resolved Trader IORs, and clean shutdown.
G8-470 adds Native Image smoke coverage for type registration, offer
registration, bounded constraint rejection, local query, import/export disabled
diagnostics, loopback IIOP/Naming exposure, and clean shutdown. G8-480 adds
the `trading-service` IDL fixture, approved-peer metadata, JVM/native dry-run
direction enumeration, `InteropScenario.tradingService()`, and structured
missing-prerequisite reports without starting peer containers or claiming live
peer compatibility. G8-490 closes the Trading Service conformance record for
the implemented local/IIOP/Native Image/dry-run subset without live peer
pass/fail claims. G8-500 splits Transaction Service / OTS into bounded
implementation slices covering coordinator/resource model, timeout policy,
local state transitions, propagation metadata, recovery boundary, IIOP
request-context boundary, Native Image smoke, interop metadata, and conformance
closure. G8-510 adds the bounded local coordinator/resource model with
immutable snapshots and stable diagnostics. The next ready roadmap task is
G8-520, the Transaction Service timeout policy.
