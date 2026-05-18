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
representation. DynamicAny, DII, DSI, object-reference Any values, peer
transport behavior, and `org.omg.CORBA.Any` compatibility remain later roadmap
work.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
