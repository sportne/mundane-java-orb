# Scope

## In scope for the complete ecosystem

- OMG IDL parser and semantic analyzer.
- `idlj`-like compiler.
- Legacy IDL-to-Java compatible generation.
- Modern generated-code mode.
- CDR, IOR, GIOP, IIOP.
- ORB core and object references.
- POA and PortableServer APIs.
- Any, TypeCode, DynamicAny, DII, DSI.
- Interface Repository or compatible static metadata bridge.
- Portable Interceptors.
- CosNaming client and server.
- Optional CORBA services in separately staged modules.
- RMI-IIOP and Java-to-IDL only after dedicated design approval.
- GraalVM Native Image support.
- OpenJDK and GraalVM JVM execution on Java 21 and Java 25.
- Interoperability tests with JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK
  ORB, and ACE/TAO.

## Out of scope until separate ADR approval

- Runtime bytecode generation.
- Reflection-driven normal invocation or normal marshaling.
- Unbounded dynamic classpath scanning.
- Copying implementation source from reference ORBs.
- Shipping with a public open-source license before legal approval.
