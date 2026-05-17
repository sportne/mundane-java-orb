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
