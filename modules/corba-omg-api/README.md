# corba-omg-api

Legacy org.omg.* and Cos* compatibility API artifact. The only artifact allowed to define org.omg.* packages.

## Current status

G10-030 expands this module into an API-only legacy compatibility surface for
non-optional pre-1.0 interop fixtures. It exposes source-compatible
`org.omg.CORBA`, `org.omg.CORBA.portable`, `org.omg.PortableServer`,
`org.omg.DynamicAny`, `org.omg.PortableInterceptor`, and `org.omg.CosNaming`
types needed by generated-style helpers, holders, stubs, POA skeletons, Naming,
DynamicAny, and interceptor sources.

This module does not implement ORB initialization, object adaptation, POA
dispatch, IIOP transport, CDR marshaling, Interface Repository lookup,
DynamicAny traversal, interceptor registration, Naming behavior, peer execution,
or optional CORBA Services. Concrete methods that would require runtime
ownership fail deterministically with `NO_IMPLEMENT` or another compatibility
system exception.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
