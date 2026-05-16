# Interoperability Requirements

| ID | Requirement | Status | Trace reference |
|---|---|---|---|
| REQ-INTEROP-001 | Test our JVM client against peer servers. | draft | CORBA-IOP-ARCH, CORBA-IOP-GIOP, CORBA-IOP-IIOP |
| REQ-INTEROP-002 | Test our native client against peer servers. | draft | CORBA-IOP-ARCH, CORBA-IOP-GIOP, CORBA-IOP-IIOP, ADR-0010 |
| REQ-INTEROP-003 | Test peer clients against our JVM server. | draft | CORBA-IOP-ARCH, CORBA-IOP-GIOP, CORBA-IOP-IIOP |
| REQ-INTEROP-004 | Test peer clients against our native server. | draft | CORBA-IOP-ARCH, CORBA-IOP-GIOP, CORBA-IOP-IIOP, ADR-0010 |
| REQ-INTEROP-005 | Include JacORB as a Java ORB peer. | draft | ADR-0006, docs/reference-implementations.md |
| REQ-INTEROP-006 | Include Eclipse GlassFish CORBA ORB as a Java/Jakarta ORB peer. | draft | ADR-0006, docs/reference-implementations.md |
| REQ-INTEROP-007 | Include JBoss OpenJDK ORB as a legacy Java/application-server peer. | draft | ADR-0006, docs/reference-implementations.md |
| REQ-INTEROP-008 | Include ACE/TAO as a C++ peer. | draft | ADR-0006, docs/reference-implementations.md |
| REQ-INTEROP-009 | Every interop failure shall produce a structured report. | draft | ADR-0006, docs/verification/interop-matrix.md |
