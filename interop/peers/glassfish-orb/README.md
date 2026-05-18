# glassfish-orb

Role: Eclipse GlassFish CORBA ORB interoperability and behavioral reference.

## Status

G6-820 artifact and license gates are approved in `peer.yaml` and
`interop/approvals/glassfish-orb.approval.yaml`.

Candidate peer: `org.glassfish.corba:glassfish-corba:5.0.0` from Maven Central.
The corresponding source repository and reference-design identity is
`eclipse-ee4j/orb`.

Dry-run commands are available through `build-image.sh`, `launch.sh`, and
`health.sh`. Real peer execution is enabled only when the approved external
cache and configured peer image prerequisites are present. Logs, IORs, and
structured reports are written under `build/interop/glassfish-orb/`.
