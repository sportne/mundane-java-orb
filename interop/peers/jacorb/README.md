# jacorb

Role: Java ORB interoperability and behavioral reference.

## Status

G6-820 artifact and license gates are approved in `peer.yaml` and
`interop/approvals/jacorb.approval.yaml`.

Candidate peer: `org.jacorb:jacorb:3.9` from Maven Central / the JacORB
project.

Dry-run commands are available through `build-image.sh`, `launch.sh`, and
`health.sh`. Real peer execution is enabled only when the approved external
cache and configured peer image prerequisites are present. Logs, IORs, and
structured reports are written under `build/interop/jacorb/`.
