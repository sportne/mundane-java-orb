# ADR-0014: Durable ORB and POA Identity

Status: accepted
Date: 2026-05-30
Decision owner: Maintainers

Approved: 2026-05-30 by G12-100 design gate implementation.

## Context

The current ORB and POA runtime supports local and loopback IIOP object
references, transient POA activation, and in-memory Naming Service state. That
is enough for the G10 peer matrix, but it does not define restart-safe ORB
identity, persistent POA identity, durable object keys, persistent IORs, or a
Naming store that can survive process restart.

Persistent POA references are already rejected or deferred because a persistent
object reference is only meaningful when the ORB identity, POA path, object id,
profile encoding, and restart rules are stable. Naming persistence has the same
dependency: a stored name-to-object mapping must not outlive the identity model
that makes the referenced object meaningful.

## Decision

Accept durable ORB and POA identity as an explicit supported design direction,
implemented only through follow-on roadmap tasks.

The durable identity model is:

- A durable ORB has an explicit `orbId`, configured at startup, never generated
  implicitly for persistent references.
- A persistent POA has a stable hierarchical POA path under that `orbId`.
- A persistent object key encodes a versioned project-owned key format
  containing `orbId`, POA path, object id bytes, and key flags.
- Transient object keys remain distinct from persistent keys and must not be
  accepted after restart as durable references.
- Persistent IORs are emitted only for persistent POA activations and must carry
  the same durable object key through KeyAddr/ProfileAddr/ReferenceAddr paths.
- Naming persistence may store object references and naming contexts only after
  the referenced IOR format is durable and restart-safe.
- Stores must be caller-configured. The runtime must not silently create a
  global persistence location.
- All on-disk formats must be bounded, versioned, and validated before
  allocation or activation.

This ADR does not approve implementation in the G12-100 task. It approves the
design direction and the staged implementation tasks that follow it.

## Consequences

- `PERSISTENT` POA lifespan can move from deterministic deferral to scoped
  implementation after the ORB identity foundation exists.
- Network IIOP dispatch must distinguish transient object keys from durable
  object keys before accepting restart-safe references.
- Stringified IOR and object URL behavior must preserve durable object keys
  without relying on process-local registry state.
- Naming persistence must be layered on durable IORs rather than storing local
  servant objects or process-local references.
- Native Image support remains first-class: durable identity cannot require
  reflection metadata, dynamic proxies, service-loader discovery, Java
  serialization metadata, runtime bytecode generation, `Unsafe`, `sun.*`, or
  `jdk.internal.*`.

## Alternatives considered

- Keep persistent POA references unsupported permanently: rejected because
  durable references are central to practical CORBA deployments.
- Reuse process-local object ids as persistent keys: rejected because they are
  not stable across restart and would make stale references ambiguous.
- Persist Naming state before durable IORs: rejected because stored bindings
  would not have restart-safe object identity.
- Use Java serialization for persistence: rejected by project architecture and
  Native Image policy.

## Specification references

- CORBA-IF-ORB
- CORBA-IF-OBJECT-REF
- CORBA-IF-POA
- CORBA-IOP-IOR
- CORBA-IOP-GIOP
- CORBA-IOP-IIOP
- NAM-13-SERVICE

## Requirements affected

- REQ-ORB-001
- REQ-POA-001
- REQ-POA-002
- REQ-IOR-001
- REQ-IOR-002
- REQ-NAM-001
- REQ-SEC-006
- REQ-NATIVE-001
- REQ-NATIVE-002
- REQ-DOC-006

## Build/test impact

No product code or tests are added by this ADR. Follow-on tasks must add unit,
loopback IIOP, restart simulation, hostile-input, Native Image, and interop
report coverage appropriate to each slice.

## Native-image impact

Durable identity must remain closed-world friendly. Persistence codecs and
stores must be explicit classes with static construction paths. No runtime
classpath scanning, reflection dispatch, dynamic proxies, Java serialization,
runtime bytecode generation, process execution in runtime modules, internal JDK
APIs, or `Unsafe` are approved.

## Interop impact

Peer interop claims require a later task that proves durable IORs survive
restart and can be consumed through approved peer scenarios. Until then,
durable identity evidence is local unit, loopback IIOP, structured-report, and
Native Image evidence only.

## Security impact

Object keys, persisted IORs, and naming stores are hostile inputs. Follow-on
tasks must bound decoded sizes, reject malformed versions and path traversal,
avoid leaking store paths or secret material in diagnostics, and distinguish
unknown, stale, and malformed persistent references deterministically.
