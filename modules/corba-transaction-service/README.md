# corba-transaction-service

Transaction Service / OTS implementation module.

## Current status

Design accepted by ADR-0021. `G8-500` split the implementation into bounded
tasks covering the coordinator/resource model, timeout policy, local state
transitions, propagation metadata, recovery boundary, IIOP request-context
boundary, Native Image smoke, interop metadata, and conformance closure.

`G8-510` adds the first local subset: a bounded in-memory coordinator/resource
model with stable transaction and resource IDs, opaque local handles, immutable
snapshots, deterministic insertion ordering, caller-configured transaction and
resource limits, enlist/delist behavior, explicit forget/removal, and stable
`TXN-*` diagnostics for duplicate, missing, stale, malformed, and limit
failures. `G8-520` adds explicit timeout policy: bounded default and maximum
timeouts, caller-requested timeout validation, caller-injected clock support,
begin-time/deadline metadata on transaction snapshots, and deterministic
expired transaction diagnostics without ambient scheduler threads or durable
logs. `G8-530` adds local state transitions for active, rollback-only,
committed, rolled-back, timeout-rolled-back, and bounded heuristic states. It
also adds deterministic local prepare/commit/rollback callbacks, resource
failure diagnostics, terminal-state cleanup, and illegal transition rejection.
`G8-540` adds bounded local propagation metadata: immutable local contexts,
deterministic project-owned text encode/decode, coordinator validation for
unknown, stale, expired, malformed, and oversized contexts, and explicit
coverage that avoids Java serialization.
`G8-550` adds the recovery boundary: durable recovery is explicitly disabled,
requests for durable logs or replay fail with stable diagnostics, and terminal
local states remain deterministic without durable replay.
`G8-560` adds a descriptor-backed loopback IIOP request-context boundary for
the supported local propagation subset: stable transaction service-context IDs
and bytes, malformed and duplicate request-context diagnostics, stale, unknown,
and expired context rejection through the local coordinator, and deterministic
clean shutdown.

Native Image, interop, durable recovery logs, XA integration, Security Service
integration, distributed peer two-phase commit, and live peer claims remain out
of scope until later promoted slices. `G8-570` is the next promoted
implementation slice for Native Image smoke coverage.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
