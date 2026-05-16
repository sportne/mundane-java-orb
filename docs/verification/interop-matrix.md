# Interoperability Matrix

## Peers

| Peer | JVM peer | Native peer | Notes |
|---|---:|---:|---|
| JacORB | yes | no | Java ORB. Our implementation must be tested in JVM and native modes against it. |
| Eclipse GlassFish CORBA ORB | yes | no | Java/Jakarta ORB. |
| JBoss OpenJDK ORB | yes | no | Legacy Java/OpenJDK ORB behavior. |
| ACE/TAO | no | yes | C++ native ORB. |

## Required directions

| Client | Server | Required |
|---|---|---|
| our JVM client | our JVM server | yes |
| our native client | our native server | yes |
| our JVM client | our native server | yes |
| our native client | our JVM server | yes |
| our JVM client | peer server | yes |
| our native client | peer server | yes |
| peer client | our JVM server | yes |
| peer client | our native server | yes |

## Scenario groups

- basic primitives, structs, enums, strings, exceptions;
- IOR, stringified IOR, object references;
- corbaloc and corbaname;
- POA policies;
- Naming Service;
- Any and TypeCode;
- valuetypes;
- GIOP versions and fragmentation;
- code set negotiation;
- Portable Interceptors;
- RMI-IIOP;
- services where applicable.
