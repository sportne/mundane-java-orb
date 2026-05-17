# CORBA 3.3 Compatibility Matrix

| Spec area | Clause / section | Requirement IDs | Status | Test IDs | Notes |
|---|---|---|---|---|---|
| ORB object model | CORBA 3.3 Interfaces section 5, The Object Model; section 8, ORB Interface | REQ-ORB-001 | not-started | unassigned | Compatibility profile; G3 assigns tests. |
| IDL relationship | IDL-42-GRAMMAR; CORBA 3.3 Interfaces section 6.2.5, OMG Interface Definition Language | REQ-IDL-001 | not-started | unassigned | Compatibility profile; G3 assigns tests. |
| CDR | CORBA 3.3 Interoperability section 9.3, CDR Transfer Syntax | REQ-CDR-001 | not-started | unassigned | Compatibility profile; G3 assigns tests. |
| GIOP | CORBA 3.3 Interoperability sections 9.2 through 9.6 | REQ-GIOP-001 | partial | `GiopMessageCodecTest` | G6-510 starts bounded GIOP 1.2 message syntax read/write behavior. TCP transport, ORB dispatch, POA lookup, peer interop, and full object-reference target forms remain future work. |
| IIOP | CORBA 3.3 Interoperability section 9.7, Internet Inter-ORB Protocol | REQ-IIOP-001 | not-started | unassigned | Compatibility profile; G3 assigns tests. |
| IOR | CORBA 3.3 Interoperability section 7.6, Object References | REQ-IOR-001 | not-started | unassigned | Compatibility profile; G3 assigns tests. |
| POA | CORBA 3.3 Interfaces section 15, The Portable Object Adapter | REQ-POA-001 | not-started | unassigned | Compatibility profile; G3 assigns tests. |
