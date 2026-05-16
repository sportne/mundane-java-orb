# Interoperability Lab

This directory defines external ORB peer manifests, shared IDL corpora, launch
scripts, and reports.

No peer implementation source is vendored here.

## G3 manifest status

G3 defines reviewable peer manifests only. The files under `interop/peers/*`
record candidate peer versions, observed license notes, clean-room restrictions,
future container inputs, launch expectations, and report outputs.

G3 does not add Dockerfiles, launch scripts, peer binaries, generated code, or
interop tests. G4 is responsible for turning these manifests into deterministic
container builds and executable peer launch scripts.

## Shared container contract

- Peer images must use deterministic names from each `peer.yaml`.
- Base images must be pinned by digest when G4 adds container build files.
- Peer source and binaries must come from external artifact/cache inputs, never
  from vendored repository content.
- Peer server and naming endpoints reserve test port `2809`.
- Launch scripts must write logs, IOR files, and structured reports to paths
  declared by the manifest.
- Health and readiness checks are a G4 script responsibility.
