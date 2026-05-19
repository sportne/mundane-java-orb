# corba-rmi-iiop

RMI-IIOP and Java-to-IDL support, staged behind dedicated ADR.

## Current status

G7-010 adds explicit Java remote-interface declaration models and deterministic
eligibility diagnostics for the first Java-to-IDL input slice. The module still
does not generate IDL or Java bindings, invoke an ORB, marshal values, perform
wire IIOP behavior, scan classpaths, or inspect application classes through
runtime reflection.

Follow-on G7 tasks own Java-to-IDL mapping, repository IDs, generated fixtures,
binding generation, local adapters, wire integration, peer interop, and Native
Image closure.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
