# corba-interop-testkit

Reusable peer-ORB orchestration and interop assertions.

## Current status

G6-820 adds process-level tests for the `interop/bin/interop-peer` artifact gate
CLI. The tests cover manifest validation, approval record validation, external
cache checksum validation, dry-run peer commands, and the rule that real peer
execution remains blocked until G6-830.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
