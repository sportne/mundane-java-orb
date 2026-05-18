# corba-dynamic

DII, DSI, DynamicAny, and dynamic metadata integration.

## Current status

G6-720 implements local, descriptor-backed DynamicAny, DII, and DSI behavior for
the current TypeCode/Any and local ORB subset.

Implemented behavior:

- immutable `DynamicAny` values over local `AnyValue` payloads;
- `DynamicAnyFactory` construction for primitive, enum, struct, exception, and
  sequence values supported by G6-710;
- `DynamicOperationCodec` for IN-parameter operation argument/result conversion;
- `DynamicInvoker` for local DII-style invocation through `LocalOrb`;
- `DynamicSkeleton` for DSI-style adaptation to `LocalInvocationDispatcher`;
- deterministic dynamic diagnostic codes for type mismatches, invalid arguments,
  unsupported parameter modes, unknown operations, unsupported TypeCodes, and
  declared user exceptions.

The module intentionally remains local-only. It does not implement OMG
compatibility APIs, wire DII/DSI requests, DynamicAny compatibility classes,
reflection-driven invocation, runtime bytecode generation, service discovery, or
peer interoperability.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
