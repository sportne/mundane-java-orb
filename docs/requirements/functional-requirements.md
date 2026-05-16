# Functional Requirements

| ID | Title | Status | Primary modules |
|---|---|---|---|
| REQ-IDL-001 | Parse OMG IDL sources with preprocessing, includes, constants, modules, interfaces, attributes, exceptions, structs, unions, enums, typedefs, sequences, arrays, valuetypes, forward declarations, and pragmas. | draft | corba-idl-parser |
| REQ-IDL-002 | Produce deterministic normalized semantic models from IDL input. | draft | corba-idl-semantics |
| REQ-IDL-003 | Emit stable diagnostic codes for parser and semantic errors. | draft | corba-idl-parser, corba-idl-semantics |
| REQ-IDLJ-001 | Provide an `idlj`-like CLI. | draft | corba-idlj-cli |
| REQ-IDLJ-002 | Generate legacy-compatible Java bindings. | draft | corba-idl-java-mapping, corba-codegen |
| REQ-IDLJ-003 | Generate modern Java bindings. | draft | corba-modern-api, corba-codegen |
| REQ-IDLJ-004 | Generate static CDR codecs and operation descriptors. | draft | corba-codegen, corba-cdr |
| REQ-CDR-001 | Encode and decode CDR with alignment, endian handling, encapsulations, primitives, structs, unions, arrays, sequences, strings, wide strings, object references, Any, TypeCode, valuetypes, and exceptions. | draft | corba-cdr |
| REQ-GIOP-001 | Implement GIOP request, reply, locate request, locate reply, close connection, message error, cancel request, and fragmentation. | draft | corba-giop |
| REQ-IIOP-001 | Implement IIOP over TCP. | draft | corba-iiop |
| REQ-IIOP-002 | Support TLS and mTLS-capable endpoints. | draft | corba-iiop |
| REQ-IOR-001 | Parse and emit IORs, IIOP profiles, and tagged components. | draft | corba-ior |
| REQ-IOR-002 | Support stringified IORs, `corbaloc`, and `corbaname`. | draft | corba-ior |
| REQ-ORB-001 | Implement ORB initialization, shutdown, initial references, object-to-string, string-to-object, narrowing, invocation, timeout, and exception behavior. | draft | corba-orb-core |
| REQ-POA-001 | Implement RootPOA and PortableServer APIs. | draft | corba-poa |
| REQ-POA-002 | Support the approved POA policy matrix. | draft | corba-poa |
| REQ-DYN-001 | Implement Any, TypeCode, DynamicAny, DII, and DSI. | draft | corba-any, corba-typecode, corba-dynamic |
| REQ-INT-001 | Implement Portable Interceptors. | draft | corba-interceptors |
| REQ-NAM-001 | Implement CosNaming client and server. | draft | corba-naming-api, corba-naming-server |
| REQ-SVC-001 | Provide separately staged modules for Trading, Event, Notification, Transaction, Security, and Time services. | draft | corba-services-core |
| REQ-RMI-001 | Implement RMI-IIOP and Java-to-IDL only after dedicated compatibility design approval. | draft | corba-rmi-iiop |
