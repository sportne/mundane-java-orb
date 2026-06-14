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
interop metadata, durable persistence, or live peer claims. `G8-460` adds
descriptor-backed loopback IIOP/Naming exposure for the supported local Trader
facade, including type operations, offer registration and withdrawal, local
query, import/export metadata listing, disabled import diagnostics, malformed
request diagnostics, stale object-key and unknown-operation diagnostics,
Naming-resolved Trader IORs, and clean shutdown. `G8-470` adds Native Image
smoke coverage for type registration, offer registration, bounded constraint
rejection, local query, import/export disabled diagnostics, loopback IIOP/Naming
exposure, and clean shutdown. It does not add interop metadata, durable
persistence, remote federation execution, or live peer claims. `G8-480` is the
metadata-only interop slice: it adds the `trading-service` IDL fixture,
approved-peer declarations, `InteropScenario.tradingService()`, JVM/native
dry-run direction enumeration, and structured missing-prerequisite reports
without starting peer containers or local live lanes. `G8-490` closes the
Trading Service conformance record for the implemented local/IIOP/Native
Image/dry-run subset and keeps live peer pass/fail claims unapproved.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
