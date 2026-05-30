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

G10-110 closes the black-box harness contract for live execution. The harness
validates approved cache entries, digest-pinned base images, Docker/Podman
availability, prepared peer images, mounted scenario IDL, IOR/log/report
directories, and deterministic failure classification before `G10-120` records
full live evidence.

G12-050 adds a project-owned broad IDL feature corpus under
`interop/idl/g12-wide/`. The corpus is local-only in this task: parser,
semantics, mapping, generated-source compilation, local JVM lane reports, and
Native Image prerequisite reports are validated without adding live peer scenario
claims or committing raw live evidence. G12-060 is responsible for selecting
which of these fixtures become approved peer scenarios.
Prepared peer images must provide executable `/interop/peer/client.sh`,
`/interop/peer/server.sh`, `/interop/peer/naming.sh`, `/interop/peer/health.sh`,
and `/interop/peer/report.sh` scripts, or equivalent `INTEROP_PEER_*_COMMAND`
environment overrides. Missing real peer commands fail with structured
infrastructure reports.

## Shared container contract

- Peer images must use deterministic names from each `peer.yaml`.
- Base images must be pinned by digest before real container builds run.
- Peer source and binaries must come from `INTEROP_ARTIFACT_CACHE`, never from
  vendored repository content. Image builds stage only approved, checksum
  validated cache entries into ignored `build/interop/container-build/` before
  invoking Docker/Podman.
- Approval records must match peer manifests and each cached artifact must match
  its source-controlled SHA-256 before real image builds are prepared.
- Peer server and naming endpoints reserve test port `2809`.
- Launch commands write logs, IOR files, and structured reports to paths
  declared by the manifest.
- Real commands validate gates first. Missing cache, runtime, image, or base
  image prerequisites produce deterministic infrastructure-failure reports.
- `run-scenario` starts the peer server as a detached container, waits for the
  peer health command, runs the peer client, and removes the server container.
- `run-direction-matrix` starts a peer server before running our JVM/native
  client lanes, and starts our JVM/native server lanes before running the peer
  client. Missing local commands, native binaries, prepared peer images, or
  scenario-capable peer commands are structured infrastructure failures.

## Commands

- `interop/bin/interop-peer help`
- `interop/bin/interop-peer validate-manifests`
- `interop/bin/interop-peer validate-gates`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache interop/bin/interop-peer validate-gates --require-cache`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache interop/bin/interop-peer validate-gates --require-cache jacorb`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache interop/bin/interop-peer prepare-cache jacorb`
- `interop/bin/interop-peer build-image --dry-run jacorb`
- `interop/bin/interop-peer launch --dry-run jacorb server`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache CONTAINER_RUNTIME=docker interop/bin/interop-peer launch jacorb server basic-idl`
- `interop/bin/interop-peer health --dry-run jacorb`
- `interop/bin/interop-peer report --dry-run jacorb`
- `interop/bin/interop-peer run-scenario --dry-run basic-idl all`
- `interop/bin/interop-peer run-scenario --dry-run rmi-iiop all`
- `interop/bin/interop-peer run-direction-matrix --dry-run basic-idl all`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache interop/bin/interop-peer run-scenario --require-live basic-idl all`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache interop/bin/interop-peer run-scenario --require-live rmi-iiop all`
- `INTEROP_ARTIFACT_CACHE=/absolute/approved/cache interop/bin/interop-peer run-direction-matrix --require-live basic-idl all`

Each peer directory also contains `build-image.sh`, `launch.sh`, and
`health.sh` wrappers that delegate to the canonical CLI.
