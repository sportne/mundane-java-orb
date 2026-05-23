# Optional Services Review

G6-D20 is a design-control split for optional CORBA services. It creates
service-specific gates and records verification expectations without adding
product tests or runtime behavior.

| Service | Roadmap gate | Requirement | Interop posture | Native Image posture | Security-review note |
|---|---|---|---|---|---|
| Trading Service | G8-D10-TRADING-SERVICE-DESIGN-GATE | REQ-SVC-010 | No peer scenario yet. A future gate must define trader import/export scenarios and structured-report fields. | Metadata remains empty until approved implementation tasks define deterministic entrypoints. | Must review constraint parsing, offer repository limits, and query resource bounds before implementation. |
| Event Service | G8-D20-EVENT-SERVICE-DESIGN-GATE | REQ-SVC-020 | No peer scenario yet. A future gate must define push/pull channel scenarios and missing-prerequisite reports. | Metadata remains empty until approved implementation tasks define deterministic entrypoints. | Must review channel fan-out limits, back pressure, and supplier/consumer failure diagnostics before implementation. |
| Notification Service | G8-D30-NOTIFICATION-SERVICE-DESIGN-GATE | REQ-SVC-030 | No peer scenario yet. A future gate must define notification, filter, QoS, and Event Service compatibility scenarios. | Metadata remains empty until approved implementation tasks define deterministic entrypoints. | Must review filter evaluation bounds, QoS policy validation, and administrative limit handling before implementation. |
| Transaction Service / OTS | G8-D40-TRANSACTION-SERVICE-DESIGN-GATE | REQ-SVC-040 | No peer scenario yet. A future gate must define coordinator/resource scenarios and report schema. | Metadata remains empty until approved implementation tasks define deterministic entrypoints. | Must review recovery, timeout, propagation, and resource cleanup assumptions before implementation. |
| Security Service / CSIv2 | G8-D50-SECURITY-SERVICE-DESIGN-GATE | REQ-SVC-050 | No peer scenario yet. A future gate must define secure invocation and CSIv2 compatibility scenarios. | Metadata remains empty until approved implementation tasks define deterministic entrypoints. | Requires dedicated credential, trust, policy, audit, and failure-disclosure review before implementation. |
| Time Service | G8-D60-TIME-SERVICE-DESIGN-GATE | REQ-SVC-060 | No peer scenario yet. A future gate must define time and interval service scenarios and report schema. | Metadata remains empty until approved implementation tasks define deterministic entrypoints. | Must review clock-source trust, overflow/precision limits, and deterministic diagnostics before implementation. |

## Default Validation

The default local validation for optional services is documentation-only:

```bash
./gradlew validateDesignControlPack qualityGate
git diff --check
```

No live peer execution is required until a service gate approves cache inputs,
digest-pinned base images, container runtime requirements, and service-specific
scenario definitions.
