# Security Requirements

| ID | Requirement | Status | Trace reference |
|---|---|---|---|
| REQ-SEC-001 | All network-originating lengths shall be validated before allocation. | draft | CORBA-IOP-CDR, CORBA-IOP-GIOP, CORBA-IOP-IIOP |
| REQ-SEC-002 | Maximum message, string, sequence, array, TypeCode recursion, and fragment sizes shall be configurable. | draft | CORBA-IOP-CDR, CORBA-IOP-GIOP |
| REQ-SEC-003 | CDR, GIOP, IIOP, IOR, `corbaloc`, and `corbaname` parsers shall have negative and fuzz tests. | draft | CORBA-IOP-CDR, CORBA-IOP-GIOP, CORBA-IOP-IIOP, CORBA-IOP-IOR, CORBA-IOP-OBJECT-URL, NAM-13-URLS |
| REQ-SEC-004 | Java serialization shall not be used for normal CORBA marshaling. | draft | CORBA-IOP-CDR, I2JAV-13-PORTABILITY |
| REQ-SEC-005 | TLS/mTLS shall be configurable without global JVM state. | draft | CORBA-IOP-SECURITY |
| REQ-SEC-006 | Object keys shall be structured and versioned; secrets shall not be embedded directly. | draft | CORBA-IOP-IOR, CORBA-IOP-IIOP |
