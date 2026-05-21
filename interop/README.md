# Interoperability Lab

This directory defines external ORB peer manifests, shared IDL corpora, launch
scripts, and reports.

No peer implementation source is vendored here.

## G6 artifact-gate status

G6-820 records approved black-box interop gates for the selected peers. The
files under `interop/peers/*` record candidate peer versions, observed license
notes, clean-room restrictions, external cache inputs, launch expectations, and
report outputs. The files under `interop/approvals/*` are the reviewed approval
records that pin artifact origin, version, SHA-256, cache path, and allowed use.

G6-830 adds environment-gated peer execution and structured report capture. The
repository still does not download peer binaries, vendor peer source, commit
peer outputs, generate peer-derived code, or add source-derived reference
implementation logic.

G7-090 adds the `rmi-iiop` scenario for the approved RMI-IIOP Calculator slice.
The scenario is gate-validated and dry-run executable by default; live peer
execution still requires approved external cache entries, digest-pinned base
images, and Docker/Podman.

## Shared container contract

- Peer images must use deterministic names from each `peer.yaml`.
- Base images must be pinned by digest before real container builds run.
- Peer source and binaries must come from `INTEROP_ARTIFACT_CACHE`, never from
  vendored repository content.
- Approval records must match peer manifests and each cached artifact must match
  its source-controlled SHA-256 before real image builds are prepared.
- Peer server and naming endpoints reserve test port `2809`.
- Launch commands write logs, IOR files, and structured reports to paths
  declared by the manifest.
- Real commands validate gates first. Missing cache, runtime, image, or base
  image prerequisites produce deterministic infrastructure-failure reports.

## Commands

- `interop/bin/interop-peer help`
- `interop/bin/interop-peer validate-manifests`
- `interop/bin/interop-peer validate-gates`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache interop/bin/interop-peer validate-gates --require-cache`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache interop/bin/interop-peer validate-gates --require-cache jacorb`
- `interop/bin/interop-peer build-image --dry-run jacorb`
- `interop/bin/interop-peer launch --dry-run jacorb server`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache CONTAINER_RUNTIME=docker interop/bin/interop-peer launch jacorb server basic-idl`
- `interop/bin/interop-peer health --dry-run jacorb`
- `interop/bin/interop-peer report --dry-run jacorb`
- `interop/bin/interop-peer run-scenario --dry-run basic-idl all`
- `interop/bin/interop-peer run-scenario --dry-run rmi-iiop all`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache interop/bin/interop-peer run-scenario --require-live basic-idl all`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache interop/bin/interop-peer run-scenario --require-live rmi-iiop all`

Each peer directory also contains `build-image.sh`, `launch.sh`, and
`health.sh` wrappers that delegate to the canonical CLI.
