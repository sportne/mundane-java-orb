# Coding Agent Rules

These rules are mandatory for any coding agent or automated implementation tool.

## Prime directive

Do not implement behavior unless the task references approved requirement IDs,
specification references, target module, allowed files, forbidden files, expected
tests, required documentation updates, and an exact acceptance command.

## Absolute restrictions

Agents must not:

1. add CORBA runtime implementation before the G6 implementation gate;
2. add public API without an ADR or approved architecture section;
3. add dependencies unless the task explicitly allows it;
4. weaken Gradle, Checkstyle, SpotBugs, Error Prone, ArchUnit, Spotless, JaCoCo,
   or native-image rules;
5. lower coverage thresholds;
6. introduce `java.lang.reflect.*` into forbidden modules;
7. introduce runtime bytecode generation;
8. introduce dynamic proxies into core modules;
9. introduce `java.io.ObjectInputStream` or `java.io.ObjectOutputStream` for
   normal CORBA marshaling;
10. use `Class.forName`, `Method.invoke`, `ServiceLoader`, `Unsafe`, `sun.*`, or
    `jdk.internal.*` in core modules without an ADR waiver;
11. hand-edit generated files;
12. implement protocol parsing without golden-wire and negative tests;
13. allocate based on network-provided lengths without explicit bounds checks;
14. copy code from JacORB, GlassFish ORB, JBoss/OpenJDK ORB, ACE/TAO, OpenJDK,
    or any other reference implementation;
15. silently ignore failed tests or skipped quality gates.

## Required task shape

Every agent task must include the fields defined in:

`docs/agent-guidance/implementation-task-template.md`

## Required output shape

Agent output must include:

- files changed;
- requirement IDs satisfied;
- docs updated;
- tests added or changed;
- acceptance command result;
- known limitations.
