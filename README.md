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

This repository is in **pre-1.0 interoperability completion state**:

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
- pre-1.0 full live peer execution has approved cache and Native Image
  prerequisites partly exercised, including limited `basic-idl` peer-command
  smoke success for JacORB, JBoss OpenJDK ORB, Eclipse GlassFish CORBA ORB, and
  ACE/TAO;
  `run-direction-matrix` now sequences peer-server/local-client and
  local-server/peer-client lanes explicitly; ACE/TAO now has tracked
  peer-specific image and clean-room command sources, with approved `basic-idl`
  and `rmi-iiop` direction matrices completed; release evidence remains blocked
  until the complete approved live scenario matrix is executed and summarized;
- the next non-human-gated roadmap task is the G10-120 live direction matrix
  evidence task;
- RMI-IIOP peer interop scenario gates and structured report paths have started
  under approved roadmap tasks, with live peer execution still
  environment-gated;
- RMI-IIOP Native Image smoke coverage, source-level Native Image metadata
  audits, hostile-input closure, and structured interop-report closure have
  started under approved roadmap tasks;
- optional Trading, Event, Notification, Transaction, Security, and Time
  Services are split into separately traceable human-gated design tasks, with
  no service behavior implemented by that split;
- the G10 pre-1.0 interoperability roadmap is defined for closing
  non-optional IDL, mapping, wire, runtime, Native Image, harness, and live peer
  execution gaps before a `1.0.0` release can be declared;
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

Build conventions live in `build-logic/` as composable Gradle convention plugins. Published and internal test modules live under `modules/`; non-published examples live under `examples/`.

## Ready roadmap tasks

The next non-human-gated roadmap task is
`docs/roadmap/tasks/g10-120-070-live-matrix-reporting-and-classification.md`. The parent
`docs/roadmap/tasks/g10-120-pre-1-0-full-interop-execution.md` remains in
progress. Local JVM/native direction commands and the Java peer matrix bootstrap
are complete, and JBoss OpenJDK ORB `basic-idl` readiness now passes; full
pre-1.0 live interop evidence remains blocked on report classification
normalization, project-owned Java RMI-IIOP defect closure, complete approved
live scenario execution, and clean-room evidence summaries.

Remaining human gates, including optional CORBA service approval, stay tracked
as `human-gate-blocked` roadmap tasks until maintainers record the relevant
decisions.
