# Dynamic CORBA Design

Dynamic CORBA includes Any, TypeCode, DynamicAny, DII, DSI, and Interface
Repository behavior.

## Native-friendly approach

Dynamic APIs shall operate over generated/static descriptors rather than Java
reflection.

```java
interface IdlTypeDescriptor<T> {
    String repositoryId();
    TypeCodeDescriptor typeCode();
    void write(CdrWriter out, T value);
    T read(CdrReader in);
}
```

## Rule

Reflection-based dynamic behavior requires an ADR and native-image metadata
review.

## G6-220 Descriptor Foundation

The first generated descriptor slice provides immutable static descriptor values
and compile-only codec surfaces for generated IDL declarations. This establishes
the metadata shape that later TypeCode, Any, DynamicAny, DII, DSI, and Interface
Repository tasks can consume without reflection or classpath scanning.

G6-220 does not implement full CORBA TypeCode behavior or functional marshaling.
Codec read/write methods fail predictably until the CDR string and aggregate
support task supplies the required wire behavior.

## G6-710 Local TypeCode and Any

G6-710 adds local TypeCode metadata and Any payload codecs over the generated
descriptor foundation:

- `modules/corba-typecode` provides immutable `IdlTypeCode` values for
  primitive, struct, enum, exception, sequence, and metadata-only interface
  TypeCodes.
- `modules/corba-any` provides `AnyValue<T>`, explicit CDR payload codecs, enum
  ordinal/name mapping, aggregate member-order encoding, and sequence payload
  support.

This is a local descriptor-backed slice. It does not encode the full CORBA wire
representation of TypeCode and does not introduce Interface Repository behavior,
object-reference Any values, reflection-driven marshaling, or ORB transport
dependencies.

## G6-720 Local DynamicAny, DII, and DSI

G6-720 adds the first local dynamic behavior over the G6-710 TypeCode/Any slice:

- `modules/corba-dynamic` provides immutable `DynamicAny` values and factories
  for supported primitive, enum, struct, exception, and sequence values.
- Dynamic invocation is descriptor-backed and IN-parameter only. OUT and INOUT
  parameters fail deterministically until a holder/out-argument model exists.
- `DynamicInvoker` adapts local dynamic requests to `LocalOrb` invocation.
- `DynamicSkeleton` adapts generated-style local dispatch to a dynamic handler.

This slice does not implement wire DII/DSI, OMG compatibility APIs,
object-reference dynamic values, peer interop execution, service-loader
discovery, reflection-driven dispatch, or runtime bytecode generation.

## G6-730 Local Static Interface Repository Metadata

G6-730 adds a local static metadata bridge for generated descriptors:

- `modules/corba-interface-repository` provides an immutable
  `StaticInterfaceRepository` indexed by repository ID, IDL scoped name, and
  mapped Java name.
- Repository lookups are explicit and closed-world; generated code emits a
  `GeneratedInterfaceRepository` source that lists descriptor constants directly.
- TypeCode construction can resolve generated field references through the
  static repository without reflection or classpath scanning.

This slice does not implement networked CORBA Interface Repository objects,
`org.omg.CORBA.Repository`, IFR mutation APIs, service-loader discovery, peer
interop, or recursive TypeCode graph handling.
