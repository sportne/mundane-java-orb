# corba-poa

Portable Object Adapter, POA managers, servants, object activation, and policy matrix.

## Current status

G6 POA-lite implementation work has started here. The module provides a minimal
in-process RootPOA-lite layer over `LocalOrb` for generated-style servant
dispatch.

Implemented behavior:

- the approved POA-lite policy profile;
- explicit servant activation with deterministic system object ids from
  `LocalOrb`;
- retained active-object-map lookup;
- generated-skeleton-style servant dispatch;
- `UNIQUE_ID` duplicate servant rejection;
- deterministic deactivation and shutdown failures.

This module does not implement full POA policy behavior, persistent object
references, user-assigned object ids, default servants, servant managers,
adapter activators, implicit activation, POA manager request queues, network
transport, generated stubs/skeletons, reflection, dynamic proxies, or peer
interoperability.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
