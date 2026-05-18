# G6 Release Hardening Closure

G6-930 is a verification-only closure task. It does not add CORBA production
behavior, public APIs, build plugins, dependencies, generated artifacts, or
reference implementation code.

## Closure evidence

| Area | Evidence | Release-hardening status |
|---|---|---|
| Compatibility profiles | Conformance matrices use `partial` for implemented slices and keep unsupported behavior in row notes. | Closed for G6 scope; full compatibility remains deferred by explicit matrix notes. |
| Conformance vocabulary | `docs/conformance/conformance-index.md` defines `partial` because the matrices use it for started but incomplete spec areas. | Closed. |
| Security and fuzz-style inputs | CDR, GIOP, IOR/object URL, IDL lexer/preprocessor/parser, and IIOP tests include deterministic hostile-input and limit-bound checks in the normal unit lane. | Closed for implemented parsers and protocol slices. |
| Performance and soak posture | Bounded repeated-read/repeated-parse smoke tests exercise deterministic hot paths without wall-clock thresholds. | Closed for non-flaky CI evidence. Absolute performance targets remain future release work. |
| Structured interop failures | Interop harness tests cover missing cache entries, missing digest-pinned base images, missing container runtime, failing container commands, dry-run behavior, and summary generation. | Closed for G6 report semantics. Live peer pass/fail remains environment-gated. |
| Native Image posture | Native Image boundary tests reject dynamic metadata and hostile mechanisms; smoke entrypoints are exercised repeatedly on the JVM before optional native compilation. | Closed for source-level and JVM smoke evidence. Native binary execution remains optional unless GraalVM is available. |
| Offline release validation | G6-920 provides the offline release validation aggregate, publication dry run, BOM alignment, and downstream consumer build. | Closed for local release hardening; public release remains human-gated. |

## Required local commands

```bash
./gradlew :modules:corba-cdr:test :modules:corba-giop:test :modules:corba-ior:test :modules:corba-idl-parser:test :modules:corba-iiop:test
./gradlew :modules:corba-interop-testkit:test :modules:corba-native-image:test
./interop/bin/interop-peer validate-manifests
./interop/bin/interop-peer validate-gates
./gradlew validateDesignControlPack qualityGate
git diff --check
```

## Optional gated commands

These commands depend on local toolchains or approved external inputs and are
not required for the default local quality gate:

```bash
./gradlew :modules:corba-native-image:nativeImageBinariesSmoke
INTEROP_ARTIFACT_CACHE=/absolute/cache interop/bin/interop-peer run-scenario --require-live basic-idl all
./gradlew offlineReleaseValidation
```

## Explicit deferrals

- Full CORBA 3.4, 3.3, 3.2, and legacy Java/CORBA compatibility is not claimed
  by G6. Matrix rows with `partial` status list the implemented subset and the
  remaining unsupported behavior.
- External ORB interoperability is report-harness-ready, but live peer execution
  still requires approved external caches, digest-pinned base images, and a
  container runtime.
- Public release, redistribution, license, dependency-policy, and final legal
  approval remain blocked by the human-gated roadmap tasks.
- RMI-IIOP Java-to-IDL and optional CORBA services remain blocked until their
  dedicated ADRs, requirements, and implementation tasks are approved.
