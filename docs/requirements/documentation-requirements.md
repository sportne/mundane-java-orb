# Documentation Requirements

| ID | Requirement | Status | Trace reference |
|---|---|---|---|
| REQ-DOC-001 | Every public API package shall have `package-info.java`. | draft | ADR-0005 |
| REQ-DOC-002 | Every published module shall have module documentation once JPMS metadata is introduced. | draft | ADR-0005, ADR-0012 |
| REQ-DOC-003 | Every public class and public method in published artifacts shall have Javadoc. | draft | ADR-0005 |
| REQ-DOC-004 | Protocol code shall document bounds, invariants, and spec references. | draft | ADR-0005, docs/specification-traceability.md |
| REQ-DOC-005 | Generated code shall include source IDL, generator version, mapping mode, and compatibility profile. | draft | ADR-0005, I2JAV-13, IDL-42 |
| REQ-DOC-006 | Every implementation PR shall update relevant docs or explicitly justify why no documentation changed. | draft | ADR-0004, ADR-0005 |
