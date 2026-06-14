# corba-trading-service

Trading Service implementation module.

## Current status

Design accepted by ADR-0020. `G8-400` split the implementation into bounded
tasks covering type repository, offer repository, constraint evaluation, local
query, import/export boundary metadata, loopback IIOP/Naming, Native Image
smoke, interop metadata, and conformance closure.

`G8-410` adds the first local subset: an in-memory service type repository with
bounded names, primitive property definitions, immutable snapshots, stable
`TRAD-*` diagnostics, and caller-configured limits. `G8-420` adds bounded
in-memory offer CRUD over registered service types, primitive property values,
immutable offer snapshots, list-by-type ordering, and deterministic diagnostics
for missing types, duplicate or missing offers, property mismatches, unsupported
values, and configured limits. `G8-430` adds a closed-world bounded constraint
parser/evaluator over primitive property maps with boolean constants,
comparison operators, `and`/`or`/`not`, parentheses, and explicit hostile-input
limits. `G8-440` adds bounded local type-scoped offer query using that
constraint evaluator, deterministic offer-ID result ordering, configured result
and query-cost limits, and clear malformed/unknown/type-mismatch diagnostics.
`G8-450` adds bounded import/export boundary metadata for future federation:
link names, directions, peer trader names, fan-out limits, duplicate/missing
link diagnostics, disabled remote-federation diagnostics, wrong-direction
diagnostics, and local-query isolation from import/export metadata. It does not
add remote federation execution, IIOP/Naming exposure, Native Image smoke,
interop metadata, durable persistence, or live peer claims. `G8-460` is the
next ready implementation slice.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
