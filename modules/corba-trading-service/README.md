# corba-trading-service

Trading Service implementation module.

## Current status

Design accepted by ADR-0020. `G8-400` split the implementation into bounded
tasks covering type repository, offer repository, constraint evaluation, local
query, import/export boundary metadata, loopback IIOP/Naming, Native Image
smoke, interop metadata, and conformance closure. `G8-410` is the first ready
implementation slice.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
