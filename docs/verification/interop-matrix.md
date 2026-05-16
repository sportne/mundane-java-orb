# Interoperability Matrix

## Peers

| Peer | Manifest | JVM peer | Native peer | Candidate origin | Notes |
|---|---|---:|---:|---|---|
| JacORB | `interop/peers/jacorb/peer.yaml` | yes | no | `org.jacorb:jacorb:3.9` | Java ORB. Our implementation must be tested in JVM and native modes against it. |
| Eclipse GlassFish CORBA ORB | `interop/peers/glassfish-orb/peer.yaml` | yes | no | `org.glassfish.corba:glassfish-corba:5.0.0` | Java/Jakarta ORB. |
| JBoss OpenJDK ORB | `interop/peers/jboss-openjdk-orb/peer.yaml` | yes | no | `org.jboss.openjdk-orb:openjdk-orb:10.1.1.Final` | Legacy Java/OpenJDK ORB behavior. |
| ACE/TAO | `interop/peers/ace-tao/peer.yaml` | no | yes | `ACE+TAO-8.0.6` | C++ native ORB. |

The G4 peer scaffolds are dry-run executable. They define the expected container
contract for later real peer execution: pinned base images, external
artifact/cache inputs only, no vendored peer source or binaries, test port
`2809`, and logs, IORs, and structured reports as outputs.

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
