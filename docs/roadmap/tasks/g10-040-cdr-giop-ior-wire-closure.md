# G10-040 CDR GIOP IOR Wire Closure

Task ID: G10-040-CDR-GIOP-IOR-WIRE-CLOSURE
Status: ready-for-implementation
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-CDR-001, REQ-GIOP-001, REQ-IIOP-001, REQ-IOR-001, REQ-IOR-002, REQ-SEC-003, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0007, ADR-0008, ADR-0010
Specification references: CORBA-IOP-CDR, CORBA-IOP-GIOP, CORBA-IOP-IIOP, CORBA-IOP-IOR, CORBA-IOP-OBJECT-URL
Target module: modules/corba-cdr, modules/corba-giop, modules/corba-ior, modules/corba-iiop, modules/corba-any, modules/corba-typecode
Allowed files: modules/corba-cdr/src/**, modules/corba-giop/src/**, modules/corba-ior/src/**, modules/corba-iiop/src/**, modules/corba-any/src/**, modules/corba-typecode/src/**, docs/architecture/cdr-giop-iiop.md, docs/conformance/corba-2.3-legacy-java-matrix.md, docs/conformance/corba-3.4-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-040-cdr-giop-ior-wire-closure.md, docs/roadmap/tasks/g10-050-network-orb-poa-dispatch.md, README.md
Forbidden files: peer artifacts, live interop reports, optional service implementation, generated production artifacts not owned by this wire closure
Expected behavior: Task type: implementation. Complete peer-facing wire behavior for object references, TypeCode marshaling, user/system exception bodies, ProfileAddr and ReferenceAddr targets, locate messages, fragments, code-set negotiation for strings/wstrings, TLS tagged components, and bounded IIOP framing.
Tests to add/update: Golden-wire, hostile-input, boundary-limit, CDR/GIOP/IOR/IIOP integration, TLS component, TypeCode/Any wire, and Native Image smoke tests.
Documentation to update: CDR/GIOP/IIOP architecture, CORBA conformance rows, Native Image matrix, roadmap index, README ready-task status, this task, and G10-050 status when complete.
Commands to run: ./gradlew :modules:corba-cdr:test :modules:corba-giop:test :modules:corba-ior:test :modules:corba-iiop:test :modules:corba-any:test :modules:corba-typecode:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: New wire constructs round-trip deterministically, reject malformed inputs with stable diagnostics, and provide the transport foundation required by ORB/POA and peer interop tasks.
Rollback notes: Revert wire codec, transport, TypeCode/Any, test, and documentation changes together.
