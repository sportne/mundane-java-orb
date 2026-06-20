# corba-services-core

Shared infrastructure for optional CORBA services.

## Current status

Design accepted by ADR-0016. Shared service behavior remains limited to the
exact contracts named by promoted optional-service tasks. `G8-510` did not need
shared runtime source changes; Transaction Service currently owns its bounded
local coordinator/resource model in `modules/corba-transaction-service`.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
