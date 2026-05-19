# ADR-0013: RMI-IIOP and Java-to-IDL Design Gate

Status: accepted
Date: 2026-05-18
Decision owner: Maintainers

Approved: 2026-05-18 by maintainer approval in the project thread.

## Context

`REQ-RMI-001` keeps RMI-IIOP and Java-to-IDL out of implementation until a
dedicated compatibility design is approved. The G6 foundation now has IDL,
repository ID, generated-code, local ORB, CDR, GIOP, IIOP, POA, dynamic, naming,
interop, native-image, and release-hardening slices. At acceptance time,
`modules/corba-rmi-iiop` was scaffold-only; later G7 tasks may add scoped
implementation slices under this ADR.

RMI-IIOP sits on the legacy Java/CORBA compatibility boundary. It can affect
repository IDs, Java type mapping, generated bindings, ORB invocation, value and
exception marshaling, interop expectations, and Native Image viability. Treating
it as a normal feature task would risk crossing multiple module boundaries before
the compatibility target is clear.

## Decision

Accept the RMI-IIOP and Java-to-IDL design gate. Implementation remains limited
to scoped roadmap tasks. The approved design scope is:

- Java-to-IDL mapping and RMI-IIOP behavior are part of the
  `LEGACY_JAVA_CORBA` compatibility profile.
- Future implementation shall reuse existing repository ID, IDL semantic model,
  generated-code, ORB, CDR/GIOP/IIOP, POA, interop, and Native Image boundaries
  instead of introducing a parallel ORB or mapping pipeline.
- `modules/corba-rmi-iiop` shall remain the staging artifact for RMI-IIOP public
  APIs and adapters once implementation tasks are approved.
- Normal runtime behavior shall not use reflection-driven invocation, runtime
  bytecode generation, dynamic proxies, classpath scanning, Java serialization
  for marshaling, `Unsafe`, `sun.*`, or `jdk.internal.*`.
- Future work must be split into narrow roadmap tasks with requirement IDs,
  specification clauses, allowed files, tests, documentation updates, Native
  Image impact, interop impact, and exact acceptance commands.

This ADR does not approve immediate implementation. It approves the design shape
used by the G7 RMI-IIOP roadmap tasks.

## Consequences

- RMI-IIOP implementation remains deferred to scoped G7 roadmap tasks.
- The first implementation tasks must be design-to-test vertical slices, such as
  repository ID/hash behavior, Java-to-IDL signature classification, generated
  IDL fixtures, or local-only invocation adapters.
- Peer interop with legacy Java ORBs must be planned before claiming
  compatibility.
- Native Image restrictions are part of the public design contract, not an
  optimization pass after implementation.
- Any request to use reflection, dynamic proxies, serialization marshaling, or
  runtime code generation requires a later ADR waiver.

## Alternatives considered

- Implement RMI-IIOP directly in `corba-orb-core`: rejected because it would mix
  legacy compatibility policy with core ORB invocation.
- Build a separate RMI-IIOP ORB path: rejected because the project uses one
  implementation with explicit compatibility profiles.
- Depend on reference ORB behavior as source guidance: rejected by ADR-0006;
  reference ORBs are black-box interop peers only.

## Specification references

- JAV2I-14
- JAV2I-14-RMI-IDL
- I2JAV-13
- CORBA-IOP
- CORBA-IF-ORB
- CORBA-IF-OBJECT-REF

## Requirements affected

- REQ-RMI-001
- REQ-IDLJ-002
- REQ-IDLJ-004
- REQ-ORB-001
- REQ-CDR-001
- REQ-GIOP-001
- REQ-IIOP-001
- REQ-INTEROP-005 through REQ-INTEROP-009
- REQ-NATIVE-001
- REQ-NATIVE-002

## Build/test impact

No build task, dependency, source set, or runtime test is added by this ADR.
Future implementation tasks must add unit, generated-fixture, interop, and
Native Image smoke coverage appropriate to their slice.

## Native-image impact

RMI-IIOP must remain compatible with the project's Native Image policy. Metadata
must be explicit and generated or configured at build time. Runtime discovery,
reflection-based invocation, dynamic proxies, bytecode generation, and Java
serialization marshaling remain forbidden without a later ADR waiver.

## Interop impact

Interop plans must include at least JacORB, Eclipse GlassFish CORBA ORB, and
JBoss OpenJDK ORB for legacy Java behavior. ACE/TAO remains relevant for IDL and
IIOP compatibility where Java-to-IDL output crosses into C++ interop scenarios.

## Security impact

Future RMI-IIOP tasks must treat Java type names, repository IDs, generated IDL,
value payloads, exception payloads, and remote object references as hostile
inputs. Bounds, diagnostics, and deterministic failures must be specified before
wire behavior is implemented.
