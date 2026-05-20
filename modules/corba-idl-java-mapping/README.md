# corba-idl-java-mapping

Legacy and modern Java language mapping rules.

## Current status

G6-160 implements the first compile-safe IDL-to-Java mapping slice.

The public mapping API converts a valid `IdlSemanticModel` into an immutable
`JavaMappingModel` for one of two explicit modes:

- `LEGACY_COMPATIBILITY`: conservative legacy-oriented naming without helper,
  holder, stub, skeleton, or POA artifacts;
- `MODERN`: the same supported semantic subset under a distinct modern package
  namespace.

This module maps modules, interfaces, operations, attributes, structs, enums,
exceptions, and constants. It does not generate source text directly and does
not define ORB runtime APIs, CDR codecs, repository IDs, `org.omg.*` APIs,
reflection metadata, or Native Image configuration.

G7-050 adds compatibility coverage for the approved RMI generated-IDL fixture so
the Java-to-IDL path remains aligned with the existing IDL-to-Java mapping model.
RMI-specific helper, holder, stub, tie, skeleton placeholder, and descriptor
surfaces remain owned by `corba-rmi-iiop`.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
