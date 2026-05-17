# CORBA 3.4 Conformance Matrix

| Spec area | Clause / section | Requirement IDs | Status | Test IDs | Notes |
|---|---|---|---|---|---|
| ORB object model | CORBA-IF-OBJECT; CORBA-IF-ORB | REQ-ORB-001 | partial | `LocalOrbTest`, `SystemExceptionTest` | G6-410 adds in-process local object references and dispatcher invocation. G6-420 adds deterministic local exception mapping to minimal CORBA system exceptions and declared user-exception wrappers. Network object references, POA integration, and wire replies remain future work. |
| IDL relationship | IDL-42-GRAMMAR; CORBA-IF-OVERVIEW | REQ-IDL-001 | not-started | unassigned | G3 assigns tests. |
| CDR | CORBA-IOP-CDR | REQ-CDR-001 | partial | `CdrPrimitiveTest`, `CdrCollectionTest` | G6-310 covers bounded primitive read/write behavior, explicit endian handling, and alignment. G6-320 adds bounded narrow strings, sequence-length helpers, fixed-array validation, octet sequences, and encapsulations. Wide strings, negotiated code sets, TypeCode, Any, object references, and GIOP/IIOP integration remain future work. |
| GIOP | CORBA-IOP-GIOP | REQ-GIOP-001 | not-started | unassigned | G3 assigns tests. |
| IIOP | CORBA-IOP-IIOP | REQ-IIOP-001 | not-started | unassigned | G3 assigns tests. |
| IOR | CORBA-IOP-IOR; CORBA-IOP-OBJECT-URL; CORBA-IF-IR | REQ-IOR-001, REQ-IOR-002 | partial | `RepositoryIdTest`, `RepositoryIdVersionTest`, `IorWireTest`, `ObjectUrlTest` | RepositoryId value rules from CORBA Interfaces 14.7 are started in `modules/corba-repository-id`. G6-330 adds bounded IOR, tagged profile, tagged component, IIOP profile body, stringified IOR, `corbaloc`, and `corbaname` value parsing in `modules/corba-ior`. ORB object references, Naming Service resolution, and GIOP/IIOP transport integration remain future work. |
| TypeCode/static metadata | CORBA-IF-TYPECODE | REQ-IDLJ-004, REQ-NATIVE-002, REQ-DYN-001 | partial | `IdlDescriptorTest`, `JavaDescriptorSourceGeneratorTest` | G6-220 starts generated static descriptor metadata and compile-only codec surfaces. Full TypeCode, Any, DynamicAny, and functional marshaling remain future work. |
| POA | CORBA-IF-POA | REQ-POA-001 | not-started | unassigned | G3 assigns tests. |
