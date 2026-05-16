# Compatibility Profiles

The project uses one primary implementation with explicit compatibility profiles.
It does not create independent ORB implementations per CORBA version.

## Profiles

| Profile | Purpose |
|---|---|
| `CORBA_3_4_FULL` | Primary normative target. |
| `CORBA_3_3_COMPAT` | Formal compatibility profile for CORBA 3.3 deltas. |
| `CORBA_3_2_COMPAT` | Formal compatibility profile for CORBA 3.2 deltas. |
| `LEGACY_JAVA_CORBA` | Practical profile for Java SE 8-era ORBs, JBoss/OpenJDK ORB, GlassFish ORB legacy behavior, and older IDL-to-Java output. |
| `TAO_CPP_INTEROP` | Practical profile for ACE/TAO C++ interoperability. |

## Profile-controlled behavior

Profiles may control:

- GIOP version negotiation and fallback;
- IOR profile interpretation;
- code set negotiation behavior;
- repository ID quirks;
- IDL feature acceptance;
- Java mapping generation quirks;
- POA policy availability;
- Portable Interceptor behavior;
- Naming Service behavior;
- valuetype behavior;
- RMI-IIOP behavior.

## Rule

A profile must be represented in a conformance matrix before implementation work
can depend on it.
