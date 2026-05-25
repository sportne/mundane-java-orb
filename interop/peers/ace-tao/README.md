# ace-tao

Role: ACE/TAO C++ ORB interoperability reference.

## Status

G6-820 artifact and license gates are approved in `peer.yaml` and
`interop/approvals/ace-tao.approval.yaml`.

Candidate peer: `ACE+TAO-8.0.6` from the DOCGroup GitHub release source
archive.

Dry-run commands are available through `build-image.sh`, `launch.sh`, and
`health.sh`. The peer now has a dedicated `Containerfile` that extracts the
approved source archive from the staged external cache, configures the Linux
ACE/TAO build, builds the full ACE/TAO tree, and compiles repo-owned clean-room
peer glue. The image does not download peer artifacts and does not vendor
ACE/TAO source or binaries into this repository.

G10-110 requires this peer to run only as a black-box container using the
approved external cache entry, a digest-pinned native base image, a prepared
`corba-interop-peer-ace-tao:8.0.6` image, mounted scenario IDL, and
Docker/Podman. Missing prerequisites are structured
`infrastructure-failure` reports, not skipped evidence.

The clean-room command surface exposes `client`, `server`, `naming`, `health`,
and `report` entrypoints. `basic-idl`, `object-reference`, `giop`, `iiop`, and
`naming` use ACE/TAO ORB or Naming Service processes for bounded live smoke
behavior. `rmi-iiop` is intentionally reported as `unsupported-scenario` until
the C++ lane has a dedicated Calculator-compatible command.
