# Native Image Requirements

| ID | Requirement | Status | Trace reference |
|---|---|---|---|
| REQ-NATIVE-001 | Representative clients, servers, tools, and services shall run as GraalVM Native Image executables. | draft | ADR-0010, docs/architecture/native-image-design.md |
| REQ-NATIVE-002 | Normal stubs, skeletons, codecs, and operation descriptors shall be generated at build time. | draft | ADR-0010, I2JAV-13-PORTABILITY, CORBA-IOP-CDR |
| REQ-NATIVE-003 | Native-image metadata shall be deterministic and reviewed like source code. | draft | ADR-0010, docs/architecture/native-image-design.md |
| REQ-NATIVE-004 | Runtime class initialization policy shall be documented for every module that participates in native-image builds. | draft | ADR-0010, docs/verification/native-image-matrix.md |
| REQ-NATIVE-005 | Native tests shall cover `idlj`, client, server, naming server, and selected interop fixtures. | draft | ADR-0010, docs/verification/native-image-matrix.md |
