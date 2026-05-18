# jboss-openjdk-orb

Role: JBoss/OpenJDK ORB legacy Java interoperability reference.

## Status

G6-820 artifact and license gates are approved in `peer.yaml` and
`interop/approvals/jboss-openjdk-orb.approval.yaml`.

Candidate peer: `org.jboss.openjdk-orb:openjdk-orb:10.1.1.Final` from Maven
Central.

Dry-run commands are available through `build-image.sh`, `launch.sh`, and
`health.sh`. Real peer execution is enabled only when the approved external
cache and configured peer image prerequisites are present. Logs, IORs, and
structured reports are written under `build/interop/jboss-openjdk-orb/`.
