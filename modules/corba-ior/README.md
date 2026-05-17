# corba-ior

IOR, IIOP profiles, tagged components, corbaloc, and corbaname parsing.

## Current status

G6 IOR implementation work has started with bounded value parsing and emitting.

Implemented behavior:

- CDR read/write for IOP `IOR`, `TaggedProfile`, and `TaggedComponent` values;
- `TAG_INTERNET_IOP` IIOP profile body parsing and emitting for IIOP 1.0, 1.1,
  1.2, and later major-version-1 profile bodies;
- deterministic preservation of unknown profile tags, unknown component tags,
  and IIOP trailing extension bytes;
- standard `IOR:` stringified object reference parsing and canonical uppercase
  emitting;
- syntax-only `corbaloc` and `corbaname` parsing with bounded URL and object-key
  handling.

This module does not start an ORB, open network connections, resolve `rir:`
initial references, contact a Naming Service, or implement GIOP request
transport.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
