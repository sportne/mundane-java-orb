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
logs. `G8-530` is the next promoted implementation slice for local state
transitions.

Completion callbacks/state transitions, propagation, recovery, IIOP, Native
Image, interop, durable recovery logs, XA integration, Security Service
integration, distributed peer two-phase commit, and live peer claims remain out
of scope until later promoted slices.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
