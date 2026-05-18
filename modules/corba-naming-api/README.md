# corba-naming-api

CosNaming API/generated-compatible support.

## Current status

G6-810 implements local CosNaming value and interface types for the in-memory
Naming Service slice.

Implemented behavior:

- immutable name components and non-empty naming names;
- local stringified-name parsing and formatting with slash, dot, and backslash
  escaping;
- object/context binding target values over local object references;
- NamingContext, list-result, and binding-iterator interfaces;
- deterministic naming diagnostics for invalid names, missing names, duplicate
  bindings, non-context traversal, non-empty destroy, destroyed contexts, closed
  iterators, and unsupported URL locations.

This module does not define legacy CosNaming compatibility APIs, networked
Naming Service objects, persistence, remote location resolution, or peer
interoperability.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
