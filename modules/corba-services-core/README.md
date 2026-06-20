# corba-services-core

Shared infrastructure for optional CORBA services.

## Current status

Design accepted by ADR-0016. Shared service behavior remains limited to the
exact contracts named by promoted optional-service tasks. `G8-510` may add only
shared support needed by the Transaction Service bounded local
coordinator/resource model.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
