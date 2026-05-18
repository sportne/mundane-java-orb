# Reference Behavior Capture

Reference behavior must be captured through black-box execution wherever possible.

## Approval-to-execution flow

G6-820 approves only artifact and license gates. Each selected peer has a
source-controlled approval record under `interop/approvals/` that records the
reviewer, review date, approval evidence identifier, artifact origin, license
status, clean-room restrictions, external cache path, and SHA-256. Before G6-830 real execution, run
`interop/bin/interop-peer validate-gates --require-cache` with an absolute
`INTEROP_ARTIFACT_CACHE`.

Real peer launch, health, and report commands remain blocked until G6-830. The
approved use is black-box interoperability through logs, IORs, wire captures,
and structured reports. Source copying, implementation transliteration, and
vendored peer source or binaries remain prohibited.

## Capture fields

```yaml
peer: jacorb
peerVersion: TBD
scenario: basic-struct-roundtrip
idl: interop/idl/basic/BasicTypes.idl
clientRuntime: our-native-jdk21
serverRuntime: peer-jvm-openjdk21
observedBehavior: TBD
wireCapture: TBD
classification: our-bug | peer-bug | spec-ambiguity | profile-mismatch | expected
reviewer: TBD
approvalRecord: interop/approvals/jacorb.approval.yaml
artifactCacheEntry: TBD
cleanRoomReviewer: TBD
```

## Clean-room rule

Behavioral observations can become tests. Reference implementation source code
must not become implementation code.
