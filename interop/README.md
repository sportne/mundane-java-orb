# Interoperability Lab

This directory defines external ORB peer manifests, shared IDL corpora, launch
scripts, and reports.

No peer implementation source is vendored here.

## G4 launch scaffold status

G4 defines dry-run-capable peer launch infrastructure. The files under
`interop/peers/*` record candidate peer versions, observed license notes,
clean-room restrictions, future container inputs, launch expectations, and
report outputs.

G4 does not download peer binaries, vendor peer source, run real ORB interop,
generate CORBA code, or add interop assertions. Real peer execution requires a
later approved task after license and artifact gates are resolved.

## Shared container contract

- Peer images must use deterministic names from each `peer.yaml`.
- Base images must be pinned by digest before real container builds run.
- Peer source and binaries must come from external artifact/cache inputs, never
  from vendored repository content.
- Peer server and naming endpoints reserve test port `2809`.
- Launch scripts must write logs, IOR files, and structured reports to paths
  declared by the manifest.
- Health and readiness checks are scaffolded as dry-run commands.

## Commands

- `interop/bin/interop-peer help`
- `interop/bin/interop-peer validate-manifests`
- `interop/bin/interop-peer build-image --dry-run jacorb`
- `interop/bin/interop-peer launch --dry-run jacorb server`
- `interop/bin/interop-peer health --dry-run jacorb`
- `interop/bin/interop-peer report --dry-run jacorb`

Each peer directory also contains `build-image.sh`, `launch.sh`, and
`health.sh` wrappers that delegate to the canonical CLI.
