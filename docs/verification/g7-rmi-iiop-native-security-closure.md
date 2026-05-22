# G7 RMI-IIOP Native and Security Closure

G7-100 is a verification-only closure task. It does not add RMI-IIOP feature
behavior, generated production artifacts, public runtime APIs, or peer
compatibility claims.

## Closure evidence

| Area | Evidence | G7 closure status |
|---|---|---|
| Native Image smoke | `RmiIiopNativeSmoke` exercises explicit RMI repository ID metadata, local loopback IIOP, `RmiIiopWireServerHandler`, `RmiIiopWireClient`, normal replies, empty user exceptions, unknown object keys, and unknown operations. | Closed for the implemented local RMI-IIOP slice. |
| Native Image metadata | Source audits cover `corba-native-image` main/smoke sources and `corba-rmi-iiop` main sources for reflection metadata, dynamic proxies, serialization metadata, classpath scanning, runtime code generation, process execution, internal JDK APIs, and `Unsafe`. | Closed; accepted metadata remains empty. |
| Hostile inputs | RMI-IIOP tests reject malformed request, reply, user-exception, and system-failure bodies with stable diagnostics and no partial trailing-octet acceptance. | Closed for the implemented G7 wire and CDR slice. |
| Structured interop reports | The `rmi-iiop` peer scenario is manifest-gated, dry-run safe, and missing-prerequisite safe. Report summaries preserve RMI-IIOP scenario report paths. | Closed for default local gates. Live peer pass/fail remains environment-gated. |
| Conformance records | Legacy Java/CORBA and IDL-to-Java matrices reference the implemented G7 tests and keep RMI-IIOP status `partial`. | Closed without overstating external compatibility. |

## Required local commands

```bash
./gradlew :modules:corba-rmi-iiop:test :modules:corba-native-image:test :modules:corba-interop-testkit:test
./interop/bin/interop-peer validate-manifests
./interop/bin/interop-peer validate-gates
./gradlew validateDesignControlPack qualityGate
git diff --check
```

## Optional gated commands

These commands depend on local GraalVM Native Image or approved external peer
inputs and are not required for the default local gate:

```bash
./gradlew :modules:corba-native-image:nativeRmiIiopSmoke
./gradlew :modules:corba-native-image:nativeImageBinariesSmoke
INTEROP_ARTIFACT_CACHE=/absolute/cache \
INTEROP_JAVA_BASE_IMAGE=example@sha256:... \
INTEROP_NATIVE_BASE_IMAGE=example@sha256:... \
interop/bin/interop-peer run-scenario --require-live rmi-iiop all
```

## Explicit deferrals

- Live RMI-IIOP peer compatibility remains dependent on approved external cache
  entries, digest-pinned base images, and container runtime availability.
- The implemented RMI-IIOP slice remains limited to the approved primitive,
  `wstring`, local adapter, local wire, and empty declared user-exception
  behavior from G7.
- Full RMI-IIOP, Java-to-IDL, valuetype, code set, object-reference, and peer
  interoperability coverage remains future roadmap work.
