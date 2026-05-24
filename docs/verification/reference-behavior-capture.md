# Reference Behavior Capture

Reference behavior must be captured through black-box execution wherever possible.

## Approval-to-execution flow

G6-820 approves only artifact and license gates. Each selected peer has a
source-controlled approval record under `interop/approvals/` that records the
reviewer, review date, approval evidence identifier, artifact origin, license
status, clean-room restrictions, external cache path, and SHA-256. Before real
execution, run `interop/bin/interop-peer validate-gates --require-cache` with an
absolute `INTEROP_ARTIFACT_CACHE`.

G6-830 real-run commands are environment-gated. They may run approved
Docker/Podman peer containers only when the external cache and configured images
are present; otherwise they write structured infrastructure-failure reports.
The approved use is black-box interoperability through logs, IORs, wire
captures, and structured reports. Source copying, implementation
transliteration, and vendored peer source or binaries remain prohibited.

G7-090 extends this flow to the `rmi-iiop` scenario. Default validation remains
dry-run and missing-prerequisite safe; live RMI-IIOP peer reports require the
approved external cache, digest-pinned base images, and container runtime. A
missing prerequisite must produce a structured `infrastructure-failure` report
instead of an implicit skip.

G7-100 closes the local RMI-IIOP report evidence by validating that dry-run
execution does not mutate outputs, missing prerequisites produce structured
`rmi-iiop` reports, and summaries include captured RMI-IIOP report paths.

G10-110 closes the black-box harness boundary used by `G10-120`. Real peer
commands are allowed only after approval records, external cache checks,
digest-pinned base-image inputs, prepared peer images, Docker/Podman, scenario
IDL mounts, and report directories validate. The harness writes deterministic
`infrastructure-failure` reports for missing prerequisites and failed peer
commands, requires prepared images to expose real peer command scripts or
explicit command environment overrides, and never commits peer source, peer
binaries, or live outputs.

The first 2026-05-24 G10-120 prerequisite attempt confirmed that missing
`INTEROP_ARTIFACT_CACHE` and missing approved cache entries are classified as
structured `infrastructure-failure` reports before live peer behavior starts.
A later approved-cache attempt confirmed cache validation, Native Image binary
smoke execution, and limited `basic-idl` peer-command smoke success for JacORB,
JBoss OpenJDK ORB, and Eclipse GlassFish CORBA ORB, with generated reports
under `build/interop/*/reports/`. That later result is still release-blocking
prerequisite evidence, not compatibility evidence, because ACE/TAO has no real
prepared peer image and the current scenario runner does not execute the
required our-JVM/our-native versus peer client/server directions.

## Capture fields

```json
{
  "peer": "jacorb",
  "peerVersion": "3.9",
  "scenario": "basic-idl",
  "idl": "interop/idl/basic/BasicTypes.idl",
  "clientRuntime": "our-jvm-jdk21",
  "serverRuntime": "peer-jvm",
  "role": "server",
  "image": "corba-interop-peer-jacorb:3.9",
  "command": "server",
  "status": "passed",
  "classification": "expected-deferral",
  "exitCode": 0,
  "stdoutPath": "build/interop/jacorb/logs/basic-idl-server.stdout.log",
  "stderrPath": "build/interop/jacorb/logs/basic-idl-server.stderr.log",
  "reportPath": "build/interop/jacorb/reports/basic-idl-server.json",
  "startedAt": "2026-05-18T00:00:00Z",
  "endedAt": "2026-05-18T00:00:01Z",
  "notes": "G10-110 container command completed"
}
```

For the G7-090 RMI-IIOP lane, `scenario` is `rmi-iiop` and `idl` is
`interop/idl/rmi-iiop/Calculator.idl`.

`status` is one of `passed`, `failed`, or `skipped`. `classification` is one of
`our-bug`, `peer-bug`, `spec-ambiguity`, `profile-mismatch`,
`infrastructure-failure`, or `expected-deferral`.

## Clean-room rule

Behavioral observations can become tests. Reference implementation source code
must not become implementation code.
