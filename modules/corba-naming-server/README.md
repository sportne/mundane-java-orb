# corba-naming-server

Standalone Naming Service implementation and distribution.

## Current status

G6-810 implements a local in-memory Naming Service server slice.

Implemented behavior:

- root NamingContext creation and registration as the `NameService` local
  initial reference;
- bind, rebind, resolve, unbind, list, iterator, and destroy behavior over
  object and context bindings;
- child naming contexts and hierarchical traversal;
- local `corbaname:rir:` resolution through the existing bounded object URL
  parser and `LocalOrb` initial references.

This module does not open GIOP/IIOP transports, resolve remote corbaname
locations, persist naming databases, discover services dynamically, expose
legacy CosNaming compatibility APIs, or run peer interoperability scenarios.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
