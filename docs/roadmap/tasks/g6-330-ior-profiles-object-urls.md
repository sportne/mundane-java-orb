# G6-330 IOR Profiles and Object URLs

Task ID: G6-330-IOR-PROFILES-OBJECT-URLS
Status: complete
Gate: G6 CDR and IOR vertical slice
Requirement IDs: REQ-IOR-001, REQ-IOR-002, REQ-SEC-003, REQ-SEC-006, REQ-DOC-004
ADR IDs: ADR-0002, ADR-0004, ADR-0005
Specification references: CORBA-IOP-IOR, CORBA-IOP-IIOP, CORBA-IOP-OBJECT-URL, NAM-13-URLS
Target module: modules/corba-ior
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-330-ior-profiles-object-urls.md, docs/roadmap/tasks/g6-410-local-object-reference-invocation.md, modules/corba-ior/build.gradle, modules/corba-ior/README.md, modules/corba-ior/src/main/**, modules/corba-ior/src/test/**, modules/corba-cdr/src/main/**, docs/conformance/corba-3.4-matrix.md, docs/conformance/naming-service-matrix.md
Forbidden files: ORB connection behavior, Naming Service server behavior, GIOP message transport
Expected behavior: Task type: implementation. Parse and emit IORs, IIOP profiles, tagged components, stringified IORs, corbaloc, and corbaname values.
Tests to add/update: Unit, spec, negative, and golden-wire/object-URL tests.
Documentation to update: IOR package docs and conformance rows.
Commands to run: ./gradlew :modules:corba-ior:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: IOR and object URL parsing is deterministic, bounded, and independent of ORB runtime startup.
Rollback notes: Revert IOR implementation, tests, and docs together.
