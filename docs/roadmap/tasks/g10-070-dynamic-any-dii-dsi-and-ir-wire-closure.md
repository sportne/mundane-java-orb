# G10-070 DynamicAny DII DSI And IR Wire Closure

Task ID: G10-070-DYNAMIC-ANY-DII-DSI-AND-IR-WIRE-CLOSURE
Status: blocked
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-DYN-001, REQ-ORB-001, REQ-IDLJ-004, REQ-NATIVE-002, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0007, ADR-0008, ADR-0010
Specification references: CORBA-IF-DYNANY, CORBA-IF-DII, CORBA-IF-DSI, CORBA-IF-IR, CORBA-IF-TYPECODE, CORBA-IOP-CDR
Target module: modules/corba-dynamic, modules/corba-any, modules/corba-typecode, modules/corba-interface-repository, modules/corba-orb-core, modules/corba-iiop
Allowed files: modules/corba-dynamic/src/**, modules/corba-any/src/**, modules/corba-typecode/src/**, modules/corba-interface-repository/src/**, modules/corba-orb-core/src/**, modules/corba-iiop/src/**, docs/architecture/dynamic-corba-design.md, docs/conformance/corba-3.4-matrix.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-070-dynamic-any-dii-dsi-and-ir-wire-closure.md, docs/roadmap/tasks/g10-100-native-image-interop-binaries.md, README.md
Forbidden files: peer artifacts, live interop reports, optional service implementation, runtime reflection-driven invocation
Expected behavior: Task type: implementation. Extend local Any, TypeCode, DynamicAny, DII, DSI, and Interface Repository behavior to peer-usable wire scenarios, including OUT/INOUT holders, object-reference Any values, recursive TypeCodes, repository lookup, and bounded mismatch diagnostics.
Tests to add/update: DynamicAny, DII, DSI, Interface Repository, Any/TypeCode wire, generated metadata, network loopback, Native Image, and hostile-input tests.
Documentation to update: Dynamic CORBA design, conformance rows, interop matrix, roadmap index, README ready-task status, this task, and G10-100 status when complete.
Commands to run: ./gradlew :modules:corba-dynamic:test :modules:corba-any:test :modules:corba-typecode:test :modules:corba-interface-repository:test :modules:corba-orb-core:test :modules:corba-iiop:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: This task remains blocked until G10-050 is complete; dynamic invocation and metadata scenarios can execute over local IIOP without runtime classpath scanning, dynamic proxies, or reflection-only dispatch.
Rollback notes: Revert dynamic, metadata, wire integration, test, and documentation changes together.
