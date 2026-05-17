# Naming Service Conformance Matrix

| Naming behavior | Clause / section | Requirement IDs | Status | Test IDs | Notes |
|---|---|---|---|---|---|
| initial naming context | NAM-13-SERVICE; NAM-13-COSNAMING | REQ-NAM-001 | not-started | unassigned | G3 assigns tests. |
| bind | NAM-13-CONTEXT, bind | REQ-NAM-001 | not-started | unassigned | G3 assigns tests. |
| rebind | NAM-13-CONTEXT, rebind | REQ-NAM-001 | not-started | unassigned | G3 assigns tests. |
| resolve | NAM-13-CONTEXT, resolving names | REQ-NAM-001 | not-started | unassigned | G3 assigns tests. |
| unbind | NAM-13-CONTEXT, unbinding names | REQ-NAM-001 | not-started | unassigned | G3 assigns tests. |
| list | NAM-13-CONTEXT, listing a naming context; NAM-13-ITERATOR | REQ-NAM-001 | not-started | unassigned | G3 assigns tests. |
| destroy | NAM-13-ITERATOR, destroy; NAM-13-CONTEXT, deleting contexts | REQ-NAM-001 | not-started | unassigned | G3 assigns tests. |
| corbaname | NAM-13-URLS; CORBA-IOP-OBJECT-URL | REQ-IOR-002 | partial | `ObjectUrlTest` | G6-330 adds syntax-only `corbaname` parsing through `modules/corba-ior`; Naming Service resolution remains future work. |
