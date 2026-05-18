# Test Taxonomy

| Tag | Purpose |
|---|---|
| `unit` | Fast local tests. |
| `architecture` | ArchUnit and architecture boundary tests. |
| `spec` | Tests tied directly to spec clauses. |
| `golden-wire` | Exact byte protocol tests. |
| `generated-code` | IDL-to-source snapshot and compile tests. |
| `integration` | Local multi-module runtime tests. |
| `interop` | External ORB tests. |
| `native-image` | GraalVM Native Image tests. |
| `slow` | Soak and stress tests. |
| `fuzz` | Parser/protocol fuzz tests. |
| `security` | Hostile input and security behavior tests. |
| `offline-build` | Offline repository and dependency validation. |

Golden fixture layout and comparison behavior are defined in
`fixture-conventions.md`.

G6-930 hostile-input regressions intentionally run in the normal unit lane.
They may also carry the `security` tag, but they do not use the excluded `fuzz`
tag because they are deterministic bounded cases rather than long-running fuzz
jobs.
