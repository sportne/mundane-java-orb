# corba-any

Any implementation and Any/TypeCode integration.

## Current status

G6-710 implements local, static-descriptor-backed Any payload support for the
current TypeCode/CDR subset.

Implemented behavior:

- immutable `AnyValue<T>` values pairing payloads with local `IdlTypeCode`
  metadata;
- `AnyValueCodec<T>` for explicit generated-descriptor-friendly CDR payload
  codecs;
- `AnyCodecs` for boolean, octet, char, integer, floating-point, string, enum,
  struct, exception, and unbounded sequence payloads;
- `AnyAggregateValue` as the explicit struct/exception value shape, encoded in
  TypeCode member order;
- deterministic `AnyException` diagnostic codes for mismatched TypeCodes,
  missing or unknown aggregate members, unsupported local TypeCode kinds, and
  invalid enum values.

The module intentionally does not marshal the full CORBA wire TypeCode
representation through the local descriptor codec. G10-040 adds a separate
wire Any codec for supported wire TypeCodes and object-reference values backed
by existing IOR values. DynamicAny, DII, DSI, peer transport behavior, and
`org.omg.CORBA.Any` runtime compatibility remain later roadmap work.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
