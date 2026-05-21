# corba-rmi-iiop

RMI-IIOP and Java-to-IDL support, staged behind dedicated ADR.

## Current status

G7-010 adds explicit Java remote-interface declaration models and deterministic
eligibility diagnostics for the first Java-to-IDL input slice. G7-020 adds an
in-memory Java-to-IDL model for eligible declarations, including modules,
interfaces, operations, parameters, declared value references, checked exception
references, and sequence-shaped Java arrays. G7-030 adds deterministic planning
of RMI repository ID strings from explicit hash and serialVersionUID metadata.
G7-040 adds deterministic generated IDL fixtures for the parser-supported subset
and validates the approved fixture through existing IDL parser and semantic
tests. G7-050 adds deterministic compile-safe Java binding source surfaces,
including RMI remote interfaces, checked user exceptions, helpers, holders,
stub/tie/skeleton placeholders, and initial binding descriptors. G7-060 adds
bounded local CDR codecs for approved primitive/String values and empty declared
user-exception payloads by repository ID. G7-070 extends generated stubs, ties,
skeletons, and binding descriptors so the approved binding slice can invoke
through in-process `LocalOrb` and `Poa` adapters. G7-080 adds bounded local JVM
RMI-IIOP wire integration over existing GIOP request/reply and `IiopClient` /
`IiopServer` paths for the approved binding slice. The module still does not
claim external peer interoperability, scan classpaths, compute Java
serialization hashes, or inspect application classes through runtime
reflection.

Follow-on G7 tasks own peer interop and Native Image closure.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
