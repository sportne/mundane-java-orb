# corba-interface-repository

Interface Repository and static metadata bridge.

## Current status

G6-730 implements a local, static Interface Repository metadata bridge over
generated descriptors.

Implemented behavior:

- immutable `StaticInterfaceRepository` instances built from explicit descriptor
  lists;
- lookup by repository ID, IDL scoped name, and mapped Java name;
- operation lookup by declaring type repository ID and operation name;
- repository-backed TypeCode conversion for generated descriptor references;
- deterministic diagnostics for duplicate descriptors, missing descriptors,
  invalid generated references, and unsupported descriptor kinds.

The module intentionally remains local-only. It does not implement networked
Interface Repository objects, `org.omg.CORBA.Repository`, mutable IFR APIs,
classpath scanning, service-loader discovery, or peer interoperability.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
