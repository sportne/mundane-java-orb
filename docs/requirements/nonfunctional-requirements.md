# Nonfunctional Requirements

| ID | Requirement | Status | Trace reference |
|---|---|---|---|
| REQ-NFR-001 | The implementation shall be generated-code-first and closed-world friendly. | draft | ADR-0010, REQ-NATIVE-002 |
| REQ-NFR-002 | Core runtime behavior shall not require runtime bytecode generation. | draft | ADR-0001, ADR-0010 |
| REQ-NFR-003 | Reflection shall be absent from core protocol, CDR, GIOP, IIOP, IOR, ORB, and POA modules unless an ADR grants a narrow exception. | draft | ADR-0004, ADR-0010 |
| REQ-NFR-004 | All network input shall be bounded before allocation. | draft | REQ-SEC-001, REQ-SEC-002, CORBA-IOP-CDR, CORBA-IOP-GIOP, CORBA-IOP-IIOP |
| REQ-NFR-005 | Build outputs shall be reproducible where technically feasible. | draft | ADR-0009, ADR-0011, REQ-BUILD-002, REQ-OFFLINE-005 |
| REQ-NFR-006 | Public compatibility APIs and modern APIs shall remain separate artifacts. | draft | ADR-0003, ADR-0012, I2JAV-13 |
| REQ-NFR-007 | Every implemented feature shall update its conformance matrix. | draft | docs/specification-traceability.md |
| REQ-NFR-008 | Every published artifact shall be usable independently where its dependency graph allows. | draft | ADR-0012, docs/architecture/artifact-model.md |
