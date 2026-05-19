# G6-D10 RMI-IIOP and Java-to-IDL

Task ID: G6-D10-RMI-IIOP-JAVA-TO-IDL
Status: complete
Gate: Deferred compatibility gate
Requirement IDs: REQ-RMI-001
ADR IDs: ADR-0002, ADR-0003, ADR-0013
Specification references: JAV2I-14, JAV2I-14-RMI-IDL
Target module: modules/corba-rmi-iiop
Allowed files: README.md, docs/adr/ADR-0013-rmi-iiop-java-to-idl.md, docs/architecture/architecture-index.md, docs/architecture/rmi-iiop-java-to-idl.md, docs/conformance/corba-2.3-legacy-java-matrix.md, docs/conformance/idl-to-java-matrix.md, docs/requirements/functional-requirements.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-d10-rmi-iiop-java-to-idl.md, docs/roadmap/tasks/g7-*.md, modules/corba-rmi-iiop/README.md
Forbidden files: RMI-IIOP runtime implementation, Java-to-IDL source generation, stub/tie/skeleton implementation, value or exception marshaling behavior, peer interop execution, Gradle dependency changes, public Java API additions, and source files under modules/corba-rmi-iiop/src/** before dedicated compatibility ADR approval
Expected behavior: Task type: design-only. Accept the dedicated RMI-IIOP and Java-to-IDL compatibility design package and create the G7 follow-on roadmap while keeping runtime implementation scoped to future tasks.
Tests to add/update: No implementation tests. Define future unit, golden-fixture, local integration, interop, and Native Image verification expectations in the ADR and architecture design.
Documentation to update: Accepted ADR, architecture design, architecture index, functional requirement row, conformance matrix status, module README, top-level README, roadmap index, G7 roadmap task files, and this task contract.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: ADR-0013 has status accepted; `docs/architecture/rmi-iiop-java-to-idl.md` defines scope, module boundaries, Native Image restrictions, security posture, interop impact, and future verification expectations; conformance matrices show RMI-IIOP and Java-to-IDL as designed but not implemented; G7 roadmap tasks cover the complete topic with exactly G7-010 ready for implementation; `modules/corba-rmi-iiop` remains scaffold-only; no runtime implementation is added by G6-D10.
Rollback notes: Revert RMI-IIOP planning docs together.
