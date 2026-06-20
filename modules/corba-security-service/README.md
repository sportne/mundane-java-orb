# corba-security-service

CORBA Security / CSIv2 implementation module.

## Current status

Design accepted by ADR-0022. `G8-600-SECURITY-SERVICE-TASK-GROUP` split the
work into bounded slices from `G8-610` through `G8-690`. `G8-610` is the only
ready implementation task and is limited to explicit credential and trust
primitives; policy, CSIv2 metadata, local policy evaluation, audit disclosure,
IIOP integration, Native Image smoke, interop metadata, and conformance closure
remain blocked behind their predecessors.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
