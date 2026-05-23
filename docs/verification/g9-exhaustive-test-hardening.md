# G9 Exhaustive Test Hardening

G9-010 is a verification-only campaign for implemented library behavior. It does
not authorize new production API, runtime, wire-format, generated-code,
artifact-coordinate, dependency, or optional-service behavior.

## Shard Policy

| Shard family | Modules | Evidence target |
|---|---|---|
| Value and core | `corba-common`, `corba-repository-id`, `corba-typecode`, `corba-any`, `corba-omg-api`, `corba-modern-api`, `corba-testkit` | Constructor contracts, value equality, immutability, enum/diagnostic stability, codec mismatch paths, and boundary values. |
| Compiler | `corba-idl-ast`, `corba-idl-parser`, `corba-idl-semantics`, `corba-idl-java-mapping`, `corba-codegen`, `corba-idlj-cli` | AST value contracts, lexer/preprocessor/parser diagnostics, semantic resolution, Java mapping names/types, source rendering, and CLI filesystem validation. |
| Runtime and protocol | `corba-cdr`, `corba-giop`, `corba-iiop`, `corba-ior`, `corba-orb-core`, `corba-poa`, `corba-naming-api`, `corba-naming-server`, `corba-dynamic`, `corba-interface-repository` | Wire/value codecs, local loopback behavior, object URL parsing, local ORB/POA dispatch, naming behavior, DynamicAny/DII/DSI, and static repository metadata. |
| RMI, interop, native | `corba-rmi-iiop`, `corba-interop-testkit`, `corba-native-image` | RMI model/mapping/generation/CDR/wire behavior, structured interop reporting without live peers, and JVM parity for native-smoke entrypoints. |

## Worker Rules

- Worker shards own disjoint test-file sets.
- Workers may add or edit assigned `src/test` files only.
- Workers must not edit production source, generated production artifacts,
  build logic, or roadmap/verification documents.
- Production defects found by tests are reported to the orchestrator for narrow
  review before any production fix is considered.

## Integration Policy

Limited integration tests remain ordinary JUnit tests in existing `src/test`
source sets. Required local tests must not rely on excluded tags or external
inputs such as live peer containers, GraalVM Native Image, approved artifact
caches, or digest-pinned base images.

## Required Validation

```bash
./gradlew test
./gradlew qualityGate
./gradlew validateDesignControlPack
git diff --check
```

## Closure Evidence

G9-010 expanded deterministic unit and limited local integration coverage across
the implemented modules through disjoint worker shards and orchestrator-owned
integration tests. Coverage was added for value contracts, codec failure paths,
IDL compiler diagnostics, source generation, CLI validation, protocol codecs,
IIOP loopback behavior, local ORB/POA/naming dispatch, DynamicAny/DII/DSI,
static interface repository metadata, RMI-IIOP model/wire paths, interop report
classification, and Native Image command/toolchain discovery.

The only production change admitted by the task was a bounded parser recovery
fix in `IdlParser` after a deterministic malformed-input test demonstrated that
an unexpected closing brace could prevent forward progress during recovery.

Validation evidence:

- Focused shard test suites passed during worker execution.
- `./gradlew test` passed after shard integration.
- JaCoCo reports were generated for 26 modules by the full test run and used as
  a signal check; no brittle line-chasing tests were added.
