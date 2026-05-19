# Functional Requirements

| ID | Title | Status | Primary modules | Specification references |
|---|---|---|---|---|
| REQ-IDL-001 | Parse OMG IDL sources with preprocessing, includes, constants, modules, interfaces, attributes, exceptions, structs, unions, enums, typedefs, sequences, arrays, valuetypes, forward declarations, and pragmas. | draft | corba-idl-parser | IDL-42-LEXICAL, IDL-42-PREPROCESSING, IDL-42-GRAMMAR, IDL-42-SCOPING |
| REQ-IDL-002 | Produce deterministic normalized semantic models from IDL input. | draft | corba-idl-semantics | IDL-42-GRAMMAR, IDL-42-SCOPING, IDL-42-PROFILES |
| REQ-IDL-003 | Emit stable diagnostic codes for parser and semantic errors. | draft | corba-idl-parser, corba-idl-semantics | Operational: diagnostics contract for IDL-42-LEXICAL, IDL-42-GRAMMAR, IDL-42-SCOPING |
| REQ-IDLJ-001 | Provide an `idlj`-like CLI. | draft | corba-idlj-cli | I2JAV-13, IDL-42 |
| REQ-IDLJ-002 | Generate legacy-compatible Java bindings. | draft | corba-idl-java-mapping, corba-codegen | I2JAV-13-MODULES, I2JAV-13-BASIC, I2JAV-13-HELPERS, I2JAV-13-TYPES, I2JAV-13-INTERFACES, I2JAV-13-SERVER, I2JAV-13-PORTABILITY |
| REQ-IDLJ-003 | Generate modern Java bindings. | draft | corba-modern-api, corba-codegen | IDL-42-GRAMMAR, IDL-42-PROFILES, I2JAV-13 as compatibility input |
| REQ-IDLJ-004 | Generate static CDR codecs and operation descriptors. | draft | corba-codegen, corba-cdr | CORBA-IOP-CDR, CORBA-IF-TYPECODE, I2JAV-13-PORTABILITY |
| REQ-CDR-001 | Encode and decode CDR with alignment, endian handling, encapsulations, primitives, structs, unions, arrays, sequences, strings, wide strings, object references, Any, TypeCode, valuetypes, and exceptions. | draft | corba-cdr | CORBA-IOP-CDR, CORBA-IF-TYPECODE, CORBA-IF-VALUES |
| REQ-GIOP-001 | Implement GIOP request, reply, locate request, locate reply, close connection, message error, cancel request, and fragmentation. | draft | corba-giop | CORBA-IOP-GIOP, CORBA-IOP-SERVICE-CONTEXT |
| REQ-IIOP-001 | Implement IIOP over TCP. | draft | corba-iiop | CORBA-IOP-IIOP |
| REQ-IIOP-002 | Support TLS and mTLS-capable endpoints. | draft | corba-iiop | CORBA-IOP-SECURITY |
| REQ-IOR-001 | Parse and emit IORs, IIOP profiles, and tagged components. | draft | corba-ior | CORBA-IOP-IOR, CORBA-IOP-IIOP |
| REQ-IOR-002 | Support stringified IORs, `corbaloc`, and `corbaname`. | draft | corba-ior | CORBA-IOP-OBJECT-URL, NAM-13-URLS |
| REQ-ORB-001 | Implement ORB initialization, shutdown, initial references, object-to-string, string-to-object, narrowing, invocation, timeout, and exception behavior. | draft | corba-orb-core | CORBA-IF-ORB, CORBA-IF-OBJECT-REF, CORBA-IF-MESSAGING |
| REQ-POA-001 | Implement RootPOA and PortableServer APIs. | draft | corba-poa | CORBA-IF-POA |
| REQ-POA-002 | Support the approved POA policy matrix. | draft | corba-poa | CORBA-IF-POA, CORBA-IF-ORB |
| REQ-DYN-001 | Implement Any, TypeCode, DynamicAny, DII, and DSI. | draft | corba-any, corba-typecode, corba-dynamic | CORBA-IF-TYPECODE, CORBA-IF-DII, CORBA-IF-DSI, CORBA-IF-DYNANY |
| REQ-INT-001 | Implement Portable Interceptors. | draft | corba-interceptors | CORBA-IF-PI |
| REQ-NAM-001 | Implement CosNaming client and server. | draft | corba-naming-api, corba-naming-server | NAM-13-SERVICE, NAM-13-COSNAMING, NAM-13-CONTEXT, NAM-13-ITERATOR, NAM-13-STRINGIFIED |
| REQ-SVC-001 | Provide separately staged modules for Trading, Event, Notification, Transaction, Security, and Time services. | draft | corba-services-core | Operational staging requirement; service-specific specs require separate ADRs |
| REQ-RMI-001 | Implement RMI-IIOP and Java-to-IDL only after dedicated compatibility design approval. | draft | corba-rmi-iiop | JAV2I-14-RMI-IDL; ADR-0013 accepted the design gate and G7 roadmap tasks stage implementation |
