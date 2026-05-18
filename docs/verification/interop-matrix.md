# Interoperability Matrix

## Peers

| Peer | Manifest | JVM peer | Native peer | Candidate origin | Gate status | Notes |
|---|---|---:|---:|---|---|---|
| JacORB | `interop/peers/jacorb/peer.yaml` | yes | no | `org.jacorb:jacorb:3.9` | G6-820 approved | Java ORB. External cache entry pinned in `interop/approvals/jacorb.approval.yaml`. |
| Eclipse GlassFish CORBA ORB | `interop/peers/glassfish-orb/peer.yaml` | yes | no | `org.glassfish.corba:glassfish-corba:5.0.0`, `eclipse-ee4j/orb` | G6-820 approved | Java/Jakarta ORB. External cache entry pinned in `interop/approvals/glassfish-orb.approval.yaml`. |
| JBoss OpenJDK ORB | `interop/peers/jboss-openjdk-orb/peer.yaml` | yes | no | `org.jboss.openjdk-orb:openjdk-orb:10.1.1.Final` | G6-820 approved | Legacy Java/OpenJDK ORB. External cache entry pinned in `interop/approvals/jboss-openjdk-orb.approval.yaml`. |
| ACE/TAO | `interop/peers/ace-tao/peer.yaml` | no | yes | `ACE+TAO-8.0.6` | G6-820 approved | C++ native ORB. External cache entry pinned in `interop/approvals/ace-tao.approval.yaml`. |

The G6-820 peer gates are dry-run executable and cache-verifiable. They define
the container contract for later real peer execution: digest-pinned base images,
external artifact/cache inputs only, no vendored peer source or binaries, test
port `2809`, and logs, IORs, and structured reports as outputs.

## Gate validation

`interop/bin/interop-peer validate-manifests` checks the structural peer
manifests. `interop/bin/interop-peer validate-gates` checks approval records,
manifest-to-approval consistency, clean-room controls, external cache layout,
SHA-256 pins, and G6-830 execution deferral. Add `--require-cache` with an
absolute `INTEROP_ARTIFACT_CACHE` to verify cached artifacts before real peer
image preparation. Supplying a peer name validates only that peer's approval and
cache entries; `build-image <peer>` uses that scoped validation.

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
