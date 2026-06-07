# Optional Services Review

G8 completes the optional-service design gates and records implementation task
groups without adding product tests or runtime behavior.

| Service | Roadmap gate | Requirement | Interop posture | Native Image posture | Security-review note |
|---|---|---|---|---|---|
| Time Service | G8-100-TIME-SERVICE-TASK-GROUP, G8-110-TIME-SERVICE-IIOP-NAMING-EXPOSURE, G8-120-TIME-SERVICE-INTEROP-METADATA, G8-130-TIME-SERVICE-LIVE-PEER-GATE, G8-140-TIME-SERVICE-CONFORMANCE-CLOSURE | REQ-SVC-060 | Approved peer manifests declare `time-service` dry-run metadata. G8-140 records live `time-service-checked` evidence for peer clients invoking our JVM and Native Image servers across JacORB, GlassFish CORBA ORB, and JBoss OpenJDK ORB. ACE/TAO and reverse peer-server directions are recorded as `unsupported-scenario` for this value-returning subset. | G8-100 smoke covers value creation, local query, interval creation, and bounded diagnostics. G8-110 smoke also covers loopback IIOP and Naming-resolved Time Service calls. G8-140 reruns SDKMAN GraalVM native smoke before live evidence. | Clock-source trust, overflow, precision loss, stale clock diagnostics, malformed request bodies, unknown object keys, unknown operations, missing live peer prerequisites, and unsupported peer profiles are bounded for the local/IIOP, metadata, and live closure slices. |
| Event Service | G8-200-EVENT-SERVICE-TASK-GROUP, G8-210-EVENT-SERVICE-CHANNEL-MODEL, G8-220-EVENT-SERVICE-LOCAL-DELIVERY, G8-230-EVENT-SERVICE-BACKPRESSURE, G8-240-EVENT-SERVICE-IIOP-NAMING-EXPOSURE, G8-250-EVENT-SERVICE-NATIVE-SMOKE | REQ-SVC-020 | Future scenarios should cover push and pull channel interoperability after missing-prerequisite reports exist. | G8-210 unit coverage verifies channel creation, admin/proxy ownership, and bounded lifecycle diagnostics. G8-220 unit coverage verifies local push/pull delivery, empty pull diagnostics, and disconnected/destroyed callback paths. G8-230 unit coverage verifies proxy limits, pending fan-out limits, failed-consumer removal, and stale-proxy diagnostics. G8-240 unit coverage verifies loopback IIOP channel/admin/proxy operations, Naming resolve, malformed object keys, unknown operations, invalid CDR bodies, disconnected proxy diagnostics, unsupported Any payloads, and clean shutdown. G8-250 Native Image smoke verifies local channel creation, push and pull delivery, bounded rejection, loopback IIOP/Naming exposure, and clean shutdown. | Channel lifecycle, configured limits, destroyed-state diagnostics, local delivery diagnostics, backpressure limits, loopback IIOP/Naming diagnostics, Native Image closed-world constraints, and failure disclosure are bounded for the local/IIOP model. |
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
