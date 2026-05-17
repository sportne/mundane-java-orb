# corba-typecode

TypeCode model, descriptors, and static metadata foundations.

## Current status

G6-220 implements the first static descriptor foundation for generated IDL
artifacts. The module provides immutable descriptor values for generated IDL
types, fields, operations, parameters, repository IDs, and compile-only codec
surfaces.

Implemented behavior:

- deterministic descriptor records for generated IDL declarations;
- IDL type and parameter-mode enums;
- `IdlCodec<T>` as the generated codec surface;
- `UnsupportedIdlCodec<T>` for predictable compile-only read/write failures;
- `./gradlew :modules:corba-typecode:nativeTypecodeDescriptorSmoke` for a
  narrow GraalVM Native Image descriptor smoke check.

Full TypeCode, Any, DynamicAny, Interface Repository integration, runtime
registries, and functional string/sequence CDR codecs remain future roadmap
work.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
