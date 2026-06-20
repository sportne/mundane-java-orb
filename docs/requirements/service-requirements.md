# Service Requirements

Optional CORBA services are staged as independent modules and independent
roadmap groups. G8 approved the service designs and implementation ordering,
but it does not approve runtime behavior until a blocked service task group is
promoted.

| ID | Title | Status | Primary modules | Specification references |
|---|---|---|---|---|
| REQ-SVC-010 | Stage Trading Service behind approved ADR-0020 and promoted G8-410 through G8-490 task slices. | implementation-started | corba-trading-service, corba-services-core | TRADE-10 |
| REQ-SVC-020 | Stage Event Service behind approved ADR-0018 and blocked G8-200 task slices. | design-approved | corba-event-service, corba-services-core | EVNT-12 |
| REQ-SVC-030 | Stage Notification Service behind approved ADR-0019 and blocked G8-300 task slices. | design-approved | corba-notification-service, corba-services-core | NOT-11 |
| REQ-SVC-040 | Stage Transaction Service / OTS behind approved ADR-0021 and promoted G8-510 through G8-590 task slices. | implementation-started | corba-transaction-service, corba-services-core | TRANS-14 |
| REQ-SVC-050 | Stage Security Service / CSIv2 behind approved ADR-0022 and promoted G8-600 task group. | design-approved | corba-security-service, corba-services-core | SEC-18, CORBA-IOP-SECURITY |
| REQ-SVC-060 | Stage Time Service behind approved ADR-0017 and blocked G8-100 task slices. | design-approved | corba-time-service, corba-services-core | TIME-11 |

## Gate Rules

- A service requirement does not authorize source, generated artifact, protocol,
  dependency, or public API changes until a blocked service task group is
  explicitly promoted to `ready-for-implementation`.
- Each approved service must record its supported specification subset,
  unsupported clauses, compatibility posture, interop scenarios, Native Image
  metadata policy, and hostile-input/security review before implementation.
- `modules/corba-services-core` may hold shared service implementation concepts
  only when a promoted task names the exact shared contract; it must not become
  a hidden implementation module for unpromoted services.
