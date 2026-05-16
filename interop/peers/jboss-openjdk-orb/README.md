# jboss-openjdk-orb

Role: JBoss/OpenJDK ORB legacy Java interoperability reference.

## Status

G3 manifest is defined in `peer.yaml`.

Candidate peer: `org.jboss.openjdk-orb:openjdk-orb:10.1.1.Final` from Maven
Central.

The manifest is non-executable. G4 must add container build files and launch
scripts that consume external artifacts without vendoring peer source or
binaries into this repository.
