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
