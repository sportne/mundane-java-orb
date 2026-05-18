# Naming Service Conformance Matrix

| Naming behavior | Clause / section | Requirement IDs | Status | Test IDs | Notes |
|---|---|---|---|---|---|
| initial naming context | NAM-13-SERVICE; NAM-13-COSNAMING | REQ-NAM-001 | partial | `LocalNamingServiceTest`, `LocalOrbTest` | G6-810 adds local `NameService` initial-reference registration and typed lookup through `LocalOrb`. Networked initial references remain future work. |
| stringified names | NAM-13-STRINGIFIED | REQ-NAM-001 | partial | `NamingNameTest` | G6-810 adds local slash/dot/backslash stringified-name parsing and formatting. Legacy compatibility API shapes remain deferred. |
| bind | NAM-13-CONTEXT, bind | REQ-NAM-001 | partial | `NamingContextTest` | G6-810 adds local object and context bind behavior with deterministic duplicate-name failures. |
| rebind | NAM-13-CONTEXT, rebind | REQ-NAM-001 | partial | `NamingContextTest` | G6-810 adds local replacement behavior for object and context targets. |
| resolve | NAM-13-CONTEXT, resolving names | REQ-NAM-001 | partial | `NamingContextTest`, `CorbanameResolverTest` | G6-810 adds local hierarchical resolution and non-context intermediate failures. |
| unbind | NAM-13-CONTEXT, unbinding names | REQ-NAM-001 | partial | `NamingContextTest` | G6-810 adds local unbind and missing-name failures. |
| list | NAM-13-CONTEXT, listing a naming context; NAM-13-ITERATOR | REQ-NAM-001 | partial | `NamingContextTest` | G6-810 adds local deterministic listing and snapshot iterator behavior. |
| destroy | NAM-13-ITERATOR, destroy; NAM-13-CONTEXT, deleting contexts | REQ-NAM-001 | partial | `NamingContextTest` | G6-810 adds local empty-context destroy and closed-iterator failures. |
| corbaname | NAM-13-URLS; CORBA-IOP-OBJECT-URL | REQ-IOR-002 | partial | `ObjectUrlTest`, `CorbanameResolverTest` | G6-330 adds syntax-only `corbaname` parsing through `modules/corba-ior`. G6-810 adds local `corbaname:rir:` resolution through `NameService`; remote IIOP and future protocol locations remain future work. |
