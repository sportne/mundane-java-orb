# Security Requirements

| ID | Requirement | Status |
|---|---|---|
| REQ-SEC-001 | All network-originating lengths shall be validated before allocation. | draft |
| REQ-SEC-002 | Maximum message, string, sequence, array, TypeCode recursion, and fragment sizes shall be configurable. | draft |
| REQ-SEC-003 | CDR, GIOP, IIOP, IOR, `corbaloc`, and `corbaname` parsers shall have negative and fuzz tests. | draft |
| REQ-SEC-004 | Java serialization shall not be used for normal CORBA marshaling. | draft |
| REQ-SEC-005 | TLS/mTLS shall be configurable without global JVM state. | draft |
| REQ-SEC-006 | Object keys shall be structured and versioned; secrets shall not be embedded directly. | draft |
