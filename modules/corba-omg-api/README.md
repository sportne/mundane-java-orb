# corba-omg-api

Legacy org.omg.* and Cos* compatibility API artifact. The only artifact allowed to define org.omg.* packages.

## Current status

G6 exception mapping work has started here. The module exposes a minimal
`org.omg.CORBA` exception compatibility surface for local invocation:

- `CompletionStatus`;
- `SystemException` plus the local slice's concrete system exceptions;
- `UserException` as a checked compatibility base.

This module does not implement ORB initialization, object references, Any,
TypeCode, helpers, holders, stubs, POA APIs, IIOP transport, or CDR exception
marshaling.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
