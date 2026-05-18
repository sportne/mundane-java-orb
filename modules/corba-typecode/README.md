# corba-typecode

TypeCode model, descriptors, and static metadata foundations.

## Current status

G6-220 implements the first static descriptor foundation for generated IDL
artifacts. G6-710 adds immutable local TypeCode metadata backed by those
descriptors for the supported Any slice.

Implemented behavior:

- deterministic descriptor records for generated IDL declarations;
- IDL type and parameter-mode enums;
- `IdlTypeCodeKind`, `IdlTypeCode`, and `IdlTypeCodeMember` for local
  descriptor-backed TypeCode metadata;
- primitive, generated descriptor, generated type-reference, and sequence
  TypeCode factories;
- `IdlCodec<T>` as the generated codec surface;
- `UnsupportedIdlCodec<T>` for predictable compile-only read/write failures;
- `./gradlew :modules:corba-typecode:nativeTypecodeDescriptorSmoke` for a
  narrow GraalVM Native Image descriptor smoke check.

Full CORBA wire TypeCode marshaling, DynamicAny, Interface Repository
integration, runtime registries, object-reference Any values, and dynamic
invocation remain future roadmap work.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
