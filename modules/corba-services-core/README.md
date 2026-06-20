# corba-services-core

Shared infrastructure for optional CORBA services.

## Current status

Design accepted by ADR-0016. Shared service behavior remains limited to the
exact contracts named by promoted optional-service tasks. `G8-510` did not need
shared runtime source changes; Transaction Service currently owns its bounded
local coordinator/resource model in `modules/corba-transaction-service`.
Security Service has been split into staged `G8-610` through `G8-690` tasks;
`G8-610`, `G8-620`, `G8-630`, `G8-640`, and `G8-650` did not need shared runtime source
changes. Shared runtime changes remain allowed only when a promoted slice names
an exact contract.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
