# corba-orb-core

ORB lifecycle, object references, invocation pipeline, initial references, and exception mapping.

## Current status

G6 local invocation work has started here. The module provides an in-process
local ORB path for generated-style clients and dispatchers.

Implemented behavior:

- local ORB creation and shutdown;
- deterministic per-ORB local object reference ids;
- in-memory dispatcher registration;
- descriptor-based local operation validation;
- generated-style local invocation without network transport.

This module does not implement GIOP/IIOP transport, POA policy behavior,
external peer interoperability, Naming Service integration, dynamic proxies,
runtime bytecode generation, or reflection-based dispatch.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
