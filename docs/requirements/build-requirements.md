# Build Requirements

| ID | Requirement | Status | Trace reference |
|---|---|---|---|
| REQ-BUILD-001 | Use Gradle Groovy DSL. | draft | ADR-0009 |
| REQ-BUILD-002 | Pin the Gradle wrapper to Gradle 9.5.1 until an ADR approves an upgrade. | draft | ADR-0009 |
| REQ-BUILD-003 | Compile production code with Java release 21. | draft | ADR-0009, docs/build/toolchain-matrix.md |
| REQ-BUILD-004 | Validate JVM execution on OpenJDK 21, OpenJDK 25, GraalVM JDK 21, and GraalVM JDK 25. | draft | ADR-0010, docs/build/toolchain-matrix.md |
| REQ-BUILD-005 | Validate Native Image execution on GraalVM JDK 21 and GraalVM JDK 25. | draft | ADR-0010, docs/verification/native-image-matrix.md |
| REQ-BUILD-006 | Use JUnit Platform/Jupiter. | draft | ADR-0007 |
| REQ-BUILD-007 | Use ArchUnit for architecture enforcement. | draft | ADR-0008, docs/architecture/module-boundaries.md |
| REQ-BUILD-008 | Use JaCoCo coverage reporting and verification. | draft | docs/verification/coverage-policy.md |
| REQ-BUILD-009 | Use Spotless, Checkstyle, SpotBugs, and Error Prone. | draft | ADR-0004, docs/architecture/build-architecture.md |
| REQ-BUILD-010 | Publish independent artifacts plus a BOM. | draft | ADR-0012, docs/architecture/artifact-model.md |
