# Optional Services Review

G8 completes the optional-service design gates and records implementation task
groups without adding product tests or runtime behavior.

| Service | Roadmap gate | Requirement | Interop posture | Native Image posture | Security-review note |
|---|---|---|---|---|---|
| Time Service | G8-100-TIME-SERVICE-TASK-GROUP | REQ-SVC-060 | Future scenarios should cover peer clients querying our Time Service and our clients querying peer Time Service endpoints after metadata exists. | Smoke must cover value creation, local query, and bounded diagnostics. | Clock-source trust, overflow, precision loss, and stale clock diagnostics must be bounded. |
| Event Service | G8-200-EVENT-SERVICE-TASK-GROUP | REQ-SVC-020 | Future scenarios should cover push and pull channel interoperability after missing-prerequisite reports exist. | Smoke must cover channel creation, connection, delivery, and bounded failure paths. | Channel fan-out, queue depth, backpressure, connection lifecycle, and failure disclosure must be bounded. |
| Notification Service | G8-300-NOTIFICATION-SERVICE-TASK-GROUP | REQ-SVC-030 | Future scenarios should cover structured events, filters, QoS rejection, and Event Service compatibility. | Smoke must cover channel creation, filter validation, QoS rejection, and local delivery. | Filter parsing/evaluation, QoS limits, administrative limits, and failure messages must be bounded. |
| Trading Service | G8-400-TRADING-SERVICE-TASK-GROUP | REQ-SVC-010 | Future scenarios should cover trader registration, lookup, query, and import/export after metadata exists. | Smoke must cover type registration, offer registration, constraint rejection, and local query. | Constraint parsing, offer counts, property values, query cost, and import/export fan-out must be bounded. |
| Transaction Service / OTS | G8-500-TRANSACTION-SERVICE-TASK-GROUP | REQ-SVC-040 | Future scenarios should cover coordinator and resource behavior only after local propagation metadata is stable. | Smoke must cover coordinator/resource state transitions and hostile timeout inputs. | Timeouts, resource cleanup, stale propagation contexts, recovery assumptions, and failure disclosure must be bounded. |
| Security Service / CSIv2 | G8-600-SECURITY-SERVICE-TASK-GROUP | REQ-SVC-050 | Future scenarios should cover CSIv2 metadata and secure invocation after local policy behavior is stable. | Smoke must cover credential rejection, policy validation, and bounded CSIv2 metadata handling. | Credential lifetime, trust roots, policy validation, audit content, downgrade behavior, and failure disclosure require dedicated review. |

## Default Validation

The default local validation for optional services is documentation-only:

```bash
./gradlew validateDesignControlPack qualityGate
git diff --check
```

No live peer execution is required until a service gate approves cache inputs,
digest-pinned base images, container runtime requirements, and service-specific
scenario definitions.
