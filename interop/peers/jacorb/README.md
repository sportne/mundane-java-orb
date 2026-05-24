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

G10-110 requires this peer to run only as a black-box container using the
approved external cache entry, a digest-pinned Java base image, a prepared
`corba-interop-peer-jacorb:3.9` image, mounted scenario IDL, and Docker/Podman.
Missing prerequisites are structured `infrastructure-failure` reports, not
skipped evidence.
