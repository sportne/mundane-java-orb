# Reference Behavior Capture

Reference behavior must be captured through black-box execution wherever possible.

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
```

## Clean-room rule

Behavioral observations can become tests. Reference implementation source code
must not become implementation code.
