# CORBA 3.4 Conformance Matrix

| Spec area | Clause / section | Requirement IDs | Status | Test IDs | Notes |
|---|---|---|---|---|---|
| ORB object model | CORBA-IF-OBJECT; CORBA-IF-ORB | REQ-ORB-001 | not-started | unassigned | G3 assigns tests. |
| IDL relationship | IDL-42-GRAMMAR; CORBA-IF-OVERVIEW | REQ-IDL-001 | not-started | unassigned | G3 assigns tests. |
| CDR | CORBA-IOP-CDR | REQ-CDR-001 | partial | `CdrPrimitiveTest` | G6-310 covers bounded primitive read/write behavior, explicit endian handling, and alignment. Strings, sequences, arrays, encapsulations, TypeCode, Any, object references, and GIOP/IIOP integration remain future work. |
| GIOP | CORBA-IOP-GIOP | REQ-GIOP-001 | not-started | unassigned | G3 assigns tests. |
| IIOP | CORBA-IOP-IIOP | REQ-IIOP-001 | not-started | unassigned | G3 assigns tests. |
| IOR | CORBA-IOP-IOR; CORBA-IOP-OBJECT-URL; CORBA-IF-IR | REQ-IOR-001 | partial | RepositoryIdTest, RepositoryIdVersionTest | RepositoryId value rules from CORBA Interfaces 14.7 are started in `modules/corba-repository-id`; IOR profiles, TypeCode integration, and object URLs remain not started. |
| POA | CORBA-IF-POA | REQ-POA-001 | not-started | unassigned | G3 assigns tests. |
