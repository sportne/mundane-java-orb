# CORBA Ecosystem for Modern Java

This repository is a design-first starting point for a complete Java CORBA
ecosystem with legacy compatibility, modern generated-code APIs, GraalVM Native
Image support, multi-ORB interoperability testing, and strong coding-agent
guardrails.

## Current repository phase

This repository is intentionally in **Gate G0/G1/G4 scaffold state**:

- governance documents exist;
- requirement and ADR templates exist;
- specification traceability structure exists;
- Gradle 9 Groovy DSL build infrastructure is scaffolded;
- JUnit Platform/Jupiter, ArchUnit, JaCoCo, Checkstyle, SpotBugs, Error Prone,
  Spotless, publishing, offline-build, and Native Image hooks are scaffolded;
- no CORBA runtime implementation has started.

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

See `AGENT_RULES.md` and `docs/agent-guidance/implementation-task-template.md`.

## Build status of this scaffold

This repository includes Gradle build files and wrapper metadata targeting Gradle
9.5.1, but the wrapper JAR is not included in this generated scaffold. In a normal
development environment, run:

```bash
./tools/bootstrap-gradle-wrapper.sh
./gradlew clean check
```

For offline environments, see `docs/build/offline-build.md` and
`tools/prepare-offline-repository.sh`.

## Critical next human review items

1. Approve or replace the placeholder license.
2. Validate the version catalog against the organization's dependency policy.
3. Generate and commit the official Gradle wrapper JAR.
4. Fill exact specification clause IDs in the conformance matrices.
5. Approve the first implementation-unlocking milestone only after gates pass.
