# POA Design

This document is the design-control source for Portable Object Adapter policy
behavior. G6-610 defines the approved policy matrix and staging boundary only;
runtime behavior begins in G6-620.

## Object Model

| Concept | Design meaning |
|---|---|
| POA path | Stable hierarchical name from the RootPOA to a child POA. The path scopes object ids and policy decisions. |
| Object id | Opaque byte sequence that identifies an object within one POA. Object ids are not globally unique without their POA path. |
| Active object map | Per-POA mapping from object id to active servant entry when `RETAIN` is active. |
| Activation | Adds a servant/object-id association according to the POA's id assignment, uniqueness, retention, and implicit activation policies. |
| Deactivation | Removes an association from the active object map and prevents later dispatch through that object id unless a policy explicitly reactivates it. |
| Servant lookup | Resolves an incoming object id to a servant by active object map, default servant, or servant manager depending on request processing policy. |
| Shutdown | Transitions dispatch to inactive behavior, rejects new activations, and releases local POA runtime state. |

## Policy Axes

| Policy axis | Values | Matrix responsibility |
|---|---|---|
| Thread policy | `ORB_CTRL_MODEL`, `SINGLE_THREAD_MODEL` | Selects whether the ORB may dispatch concurrently or whether the POA serializes requests. |
| Lifespan policy | `TRANSIENT`, `PERSISTENT` | Selects whether object references survive the POA/ORB process lifetime. |
| Object id uniqueness policy | `UNIQUE_ID`, `MULTIPLE_ID` | Selects whether one servant may be active under more than one object id. |
| Object id assignment policy | `USER_ID`, `SYSTEM_ID` | Selects whether application code or the POA assigns object ids. |
| Servant retention policy | `RETAIN`, `NON_RETAIN` | Selects whether the POA stores servant/object-id associations. |
| Request processing policy | `USE_ACTIVE_OBJECT_MAP_ONLY`, `USE_DEFAULT_SERVANT`, `USE_SERVANT_MANAGER` | Selects the lookup path after a request reaches the POA. |
| Implicit activation policy | `IMPLICIT_ACTIVATION`, `NO_IMPLICIT_ACTIVATION` | Selects whether eligible servant operations may create an activation automatically. |

## Compatibility Rules

| Rule | Valid combination | Invalid or deferred combination | Reason |
|---|---|---|---|
| Implicit activation requires retained system ids | `IMPLICIT_ACTIVATION` with `SYSTEM_ID` and `RETAIN` | `IMPLICIT_ACTIVATION` with `USER_ID` or `NON_RETAIN` | The POA must be able to allocate an object id and remember the resulting activation. |
| Active object map lookup requires retention | `USE_ACTIVE_OBJECT_MAP_ONLY` with `RETAIN` | `USE_ACTIVE_OBJECT_MAP_ONLY` with `NON_RETAIN` | A non-retaining POA has no active object map entry to consult during dispatch. |
| Servant manager type follows retention | `USE_SERVANT_MANAGER` with `RETAIN` uses `ServantActivator`; `USE_SERVANT_MANAGER` with `NON_RETAIN` uses `ServantLocator` | Mixing the wrong manager type with the retention mode | Retained servants need activation/incarnation lifecycle; non-retained servants need per-request preinvoke/postinvoke lifecycle. |
| Default servant is an alternate lookup path | `USE_DEFAULT_SERVANT` with `RETAIN` or `NON_RETAIN` | Default servant combined with servant manager for the same POA | A POA has one request-processing strategy. |
| Unique servants reject duplicate activation | `UNIQUE_ID` | Reusing one servant for multiple active ids | `UNIQUE_ID` enforces a one-servant-to-one-object-id active mapping. |
| Multiple ids allow duplicate servant activation | `MULTIPLE_ID` | None by itself | `MULTIPLE_ID` permits the same servant instance under more than one object id. |
| Persistent objects require durable identity design | `PERSISTENT` | Implementing persistent references without durable POA/ORB identity and restart rules | Persistent object references must be meaningful beyond one process lifetime. |
| Single-threaded POAs require dispatch serialization | `SINGLE_THREAD_MODEL` | Treating it as the same as `ORB_CTRL_MODEL` | The POA must preserve single-threaded dispatch semantics for that adapter. |

## Request Processing Matrix

| Servant retention | Request processing | Lookup behavior | Servant manager type | G6 stage |
|---|---|---|---|---|
| `RETAIN` | `USE_ACTIVE_OBJECT_MAP_ONLY` | Resolve object id in the active object map; reject unknown ids. | None | POA-lite in G6-620 |
| `RETAIN` | `USE_DEFAULT_SERVANT` | Prefer active object map where applicable, otherwise dispatch to the configured default servant. | None | Full POA in G6-630 |
| `RETAIN` | `USE_SERVANT_MANAGER` | Use active object map when present; ask a `ServantActivator` to incarnate missing retained entries. | `ServantActivator` | Full POA in G6-630 |
| `NON_RETAIN` | `USE_DEFAULT_SERVANT` | Dispatch each request to the configured default servant without recording an active entry. | None | Full POA in G6-630 |
| `NON_RETAIN` | `USE_SERVANT_MANAGER` | Ask a `ServantLocator` before each request and call postinvoke after dispatch. | `ServantLocator` | Full POA in G6-630 |
| `NON_RETAIN` | `USE_ACTIVE_OBJECT_MAP_ONLY` | Invalid. | None | Rejected by policy validation |

## POA Manager States

| State | Request effect | G6 stage |
|---|---|---|
| Active | Dispatch eligible requests according to the POA policy set. | POA-lite supports this state. |
| Holding | Accept requests but hold dispatch until the manager becomes active or another terminal decision occurs. | Full POA in G6-630. |
| Discarding | Reject or discard new requests according to the future wire/runtime mapping. | Full POA in G6-630. |
| Inactive | Reject new requests and activations; release local runtime state during shutdown/destroy. | POA-lite supports deterministic inactive rejection. |

POA-lite does not implement request queues. Its manager model is intentionally
limited to active dispatch and inactive shutdown rejection.

## Policy Combination Ownership Matrix

| Combination area | POA-lite in G6-620 | Full POA in G6-630 | Invalid combination |
|---|---|---|---|
| Thread policy | `ORB_CTRL_MODEL` | `SINGLE_THREAD_MODEL` | None at policy validation time. |
| Lifespan policy | `TRANSIENT` | `PERSISTENT` | Persistent references without durable POA/ORB identity are rejected until implemented. |
| Object id uniqueness | `UNIQUE_ID` | `MULTIPLE_ID` | None at policy validation time. |
| Object id assignment | `SYSTEM_ID` | `USER_ID` | None at policy validation time. |
| Servant retention | `RETAIN` | `NON_RETAIN` | None by itself; invalidity depends on request processing and implicit activation. |
| Request processing | `USE_ACTIVE_OBJECT_MAP_ONLY` with `RETAIN` | `USE_DEFAULT_SERVANT`; `USE_SERVANT_MANAGER` | `USE_ACTIVE_OBJECT_MAP_ONLY` with `NON_RETAIN`. |
| Implicit activation | `NO_IMPLICIT_ACTIVATION` | `IMPLICIT_ACTIVATION` with `SYSTEM_ID + RETAIN` | `IMPLICIT_ACTIVATION` with `USER_ID` or `NON_RETAIN`. |
| Servant manager | None | `ServantActivator` for `RETAIN`; `ServantLocator` for `NON_RETAIN` | Manager type that does not match servant retention policy. |
| Adapter activator | None | Child POA lookup and creation policy | Any behavior that creates a POA outside approved adapter activator rules. |
| POA manager behavior | Active dispatch and inactive rejection | Holding queues, discarding, full inactive semantics | Treating holding/discarding as active dispatch. |

## Staged Boundary

### POA-Lite Approved Profile

G6-620 may implement only this profile:

| Axis | Approved value |
|---|---|
| Thread policy | `ORB_CTRL_MODEL` |
| Lifespan policy | `TRANSIENT` |
| Object id uniqueness policy | `UNIQUE_ID` |
| Object id assignment policy | `SYSTEM_ID` |
| Servant retention policy | `RETAIN` |
| Request processing policy | `USE_ACTIVE_OBJECT_MAP_ONLY` |
| Implicit activation policy | `NO_IMPLICIT_ACTIVATION` |

The POA-lite dispatch path is:

```text
generated skeleton -> POA-lite dispatch -> active object map -> servant
```

POA-lite is allowed to create deterministic local object ids, activate servants
explicitly, dispatch through generated-style operation metadata, reject unknown
object ids, and shut down idempotently. It must not implement persistent object
references, implicit servant activation, default servants, servant managers,
servant locators, adapter activators, full POA manager queuing, generated-code
changes, or peer interoperability behavior.

### Full POA Expansion

G6-630 owns the remaining policy combinations and must either implement or
explicitly reject each unsupported combination with deterministic diagnostics.
That includes:

- `SINGLE_THREAD_MODEL`;
- `PERSISTENT`;
- `MULTIPLE_ID`;
- `USER_ID`;
- `NON_RETAIN`;
- `USE_DEFAULT_SERVANT`;
- `USE_SERVANT_MANAGER`;
- `IMPLICIT_ACTIVATION`;
- `ServantActivator` and `ServantLocator` lifecycle behavior;
- adapter activators and child POA lookup behavior;
- holding and discarding POA manager semantics.

## Future Test Coverage

| Test ID | Stage | Coverage intent |
|---|---|---|
| `PoaPolicyMatrixTest` | G6-620 | Validates the POA-lite policy profile and rejects known invalid combinations such as `NON_RETAIN + USE_ACTIVE_OBJECT_MAP_ONLY`. |
| `PoaLiteDispatchTest` | G6-620 | Covers explicit activation, system object id assignment, active object map lookup, generated-style servant dispatch, unknown object ids, and shutdown. |
| `PoaPolicyCombinationTest` | G6-630 | Exercises the full policy matrix, including default servants, servant managers, implicit activation, and persistent/deferred combinations. |
| `PoaManagerStateTest` | G6-630 | Covers active, holding, discarding, and inactive state transitions and request effects. |
| `PoaServantManagerTest` | G6-630 | Covers `ServantActivator` and `ServantLocator` lifecycle behavior. |
