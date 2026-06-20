# corba-transaction-service

Transaction Service / OTS implementation module.

## Current status

Design accepted by ADR-0021. `G8-500` split the implementation into bounded
tasks covering the coordinator/resource model, timeout policy, local state
transitions, propagation metadata, recovery boundary, IIOP request-context
boundary, Native Image smoke, interop metadata, and conformance closure.

`G8-510` is the first promoted implementation slice. It adds only the bounded
local coordinator/resource model; timeout, completion, propagation, recovery,
IIOP, Native Image, interop, durable recovery logs, XA integration, Security
Service integration, distributed peer two-phase commit, and live peer claims
remain out of scope until later promoted slices.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
