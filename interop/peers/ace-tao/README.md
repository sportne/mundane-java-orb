# ace-tao

Role: ACE/TAO C++ ORB interoperability reference.

## Status

G6-820 artifact and license gates are approved in `peer.yaml` and
`interop/approvals/ace-tao.approval.yaml`.

Candidate peer: `ACE+TAO-8.0.6` from the DOCGroup GitHub release source
archive.

Dry-run commands are available through `build-image.sh`, `launch.sh`, and
`health.sh`. Real peer execution is enabled only when the approved external
cache and configured peer image prerequisites are present. Logs, IORs, and
structured reports are written under `build/interop/ace-tao/`.

G10-110 requires this peer to run only as a black-box container using the
approved external cache entry, a digest-pinned native base image, a prepared
`corba-interop-peer-ace-tao:8.0.6` image, mounted scenario IDL, and
Docker/Podman. Missing prerequisites are structured
`infrastructure-failure` reports, not skipped evidence.
