# corba-trading-service

Trading Service implementation module.

## Current status

Design accepted by ADR-0020. `G8-400` split the implementation into bounded
tasks covering type repository, offer repository, constraint evaluation, local
query, import/export boundary metadata, loopback IIOP/Naming, Native Image
smoke, interop metadata, and conformance closure.

`G8-410` adds the first local subset: an in-memory service type repository with
bounded names, primitive property definitions, immutable snapshots, stable
`TRAD-*` diagnostics, and caller-configured limits. It does not add offers,
constraints, query behavior, import/export behavior, IIOP/Naming exposure,
Native Image smoke, interop metadata, durable persistence, or live peer claims.
`G8-420` is the next ready implementation slice.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
