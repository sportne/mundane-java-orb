# corba-modern-api

Modern generated-code-facing public API.

## Current status

G6 local invocation work has started here. The module exposes explicit
generated-code-facing request and dispatcher contracts for in-process calls.

Implemented behavior:

- immutable local invocation request values;
- generated-skeleton-style local invocation dispatch interface;
- static descriptor-based operation metadata handoff to ORB core.

This module does not implement ORB lifecycle, network transport, dynamic
proxies, runtime bytecode generation, or reflection-based dispatch.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
