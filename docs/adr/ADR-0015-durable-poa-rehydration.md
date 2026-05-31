# ADR-0015: Durable POA Rehydration

Status: accepted
Date: 2026-05-31
Decision owner: Maintainers

Approved: 2026-05-31 by G13-040 design gate implementation.

## Context

ADR-0014 accepted durable ORB and POA identity and the G12 durable identity
tasks implemented durable ORB ids, persistent POA object keys, persistent IOR
round trips, and caller-configured Naming persistence. G13-010 then proved that
the local persistent IOR and persistent Naming claims survive across forked JVM
server processes when callers recreate the same durable identity, endpoint,
POA path, object id, servant binding, and Naming store.

That evidence is intentionally caller-managed. A restarted process must recreate
the relevant POA hierarchy and activate servants before old durable IORs can
dispatch. That model is enough for explicit tests and simple applications, but
it does not define how the runtime should handle a valid durable object key for
a POA that is not currently active, or how POA servant-manager policy should
participate in restart-time object resolution.

## Decision

Approve POA-managed durable rehydration as the design direction for follow-on
implementation tasks.

Durable POA rehydration means the runtime may resolve a valid durable object
key by locating or activating the addressed persistent POA path, then applying
the POA's normal request-processing policy to the durable object id. It does
not mean the ORB persists servants, application state, Java objects, adapter
instances, or implementation classes.

The responsibility split is:

- Callers configure a durable `OrbIdentity`, stable endpoint policy, and any
  required Naming store explicitly.
- Callers register the persistent POA paths they are willing to rehydrate,
  together with explicit adapter activation and servant-manager factories.
- Callers own servant state, backups, application databases, authorization
  policy, and operational retention outside the ORB.
- The POA runtime owns bounded durable-key decoding, ORB id validation, POA path
  registry lookup, adapter activation dispatch, servant-manager invocation, and
  deterministic system-exception mapping.
- The IIOP bridge owns preserving opaque object-key octets and routing them to
  the ORB/POA durable-key lookup path without parsing `MJOK`.

The approved lookup order for a persistent IIOP request is:

1. Preserve the incoming object key as opaque octets through IOR, GIOP
   target-address, and IIOP server routing.
2. Ask ORB/POA durable-key lookup to decode `MJOK` version 1 within existing
   size limits.
3. Reject malformed keys, wrong ORB ids, path traversal, unsupported versions,
   oversized fields, and transient keys before adapter activation.
4. Locate the active POA by durable path, or ask the registered adapter
   activation path to create that POA if the path is approved for rehydration.
5. Apply the POA manager state and request-processing policy.
6. Resolve the object id through the active object map, default servant, or
   servant manager as allowed by that POA's policies.
7. Return deterministic stale-object or adapter-inactive system exceptions when
   a valid durable key names an absent object or inactive adapter.

Servant-manager behavior remains policy-driven. `RETAIN` with
`USE_SERVANT_MANAGER` uses a `ServantActivator` to incarnate missing durable
object ids and then records the resulting active object map entry. `NON_RETAIN`
with `USE_SERVANT_MANAGER` uses a `ServantLocator` per request only if a later
task explicitly approves persistent non-retain behavior. `USE_ACTIVE_OBJECT_MAP_ONLY`
does not rehydrate servants; it dispatches only if the object id is already
active. `USE_DEFAULT_SERVANT` may dispatch a validated durable object id to the
configured default servant, but that servant remains caller-supplied runtime
state.

The implementation must remain Native Image friendly. Registries, factories,
and servant managers must be explicit runtime objects. Rehydration must not
require reflection metadata, classpath scanning, service-loader discovery,
dynamic proxies, Java serialization, runtime bytecode generation, `Unsafe`,
`sun.*`, or `jdk.internal.*`.

All durable keys are hostile input. Malformed keys, wrong-ORB keys, unregistered
paths, path traversal attempts, unsupported versions, oversized values, inactive
adapters, stale object ids, and servant-manager failures must produce bounded,
deterministic diagnostics without leaking store paths or secret material.

## Consequences

- The current caller-managed restart model remains valid for explicit
  activation flows.
- Follow-on implementation tasks must add a persistent POA path registry,
  adapter activation lookup, servant-manager rehydration behavior, and IIOP
  routing into the durable-key lookup path.
- The ORB still does not persist servants or application state. Rehydration is a
  dispatch and activation contract, not an object database.
- Live peer durable IOR claims remain deferred until local rehydration behavior
  and hostile-key diagnostics are implemented and tested.

## Alternatives considered

- Keep all durable POA restart behavior caller-managed: rejected because old
  durable IORs should be able to reach approved adapter activation and
  servant-manager policy instead of requiring every servant to be preactivated.
- Persist servants directly in the ORB: rejected because it violates the
  project Native Image, security, and Java serialization constraints and would
  blur application state ownership.
- Let IIOP parse `MJOK` directly: rejected because protocol modules must
  preserve object keys as opaque octets and avoid depending on ORB/POA runtime
  identity internals.

## Specification references

- CORBA-IF-ORB
- CORBA-IF-OBJECT-REF
- CORBA-IF-POA
- CORBA-IOP-IOR
- CORBA-IOP-GIOP
- CORBA-IOP-IIOP

## Requirements affected

- REQ-ORB-001
- REQ-POA-001
- REQ-POA-002
- REQ-IOR-001
- REQ-IOR-002
- REQ-SEC-006
- REQ-NATIVE-002
- REQ-DOC-006

## Build/test impact

This ADR adds no product code or product tests. Follow-on tasks must add unit,
loopback IIOP, restart, hostile-key, and Native Image evidence appropriate to
the approved implementation slices.

## Native-image impact

The approved design is closed-world friendly because activation paths and
servant managers are caller-registered explicit objects. No reflection,
dynamic class discovery, Java serialization metadata, runtime bytecode
generation, internal JDK APIs, or `Unsafe` are approved.

## Interop impact

Peer ORBs see only ordinary IORs and opaque object-key bytes. Live peer tasks
may claim that peers preserve opaque durable keys when invoking a restarted
mundane Java ORB server, but peers are not expected to understand `MJOK`.

## Security impact

Durable object keys are hostile input. Lookup must validate bounds and
namespace before activation side effects, distinguish malformed, wrong-ORB,
unregistered-path, inactive-adapter, and stale-object cases deterministically,
and avoid exposing filesystem or secret material in diagnostics.
