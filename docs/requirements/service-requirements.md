# Service Requirements

Optional CORBA services are staged as independent modules and independent
roadmap gates. G6-D20 records the split only; it does not approve service
behavior.

| ID | Title | Status | Primary modules | Specification references |
|---|---|---|---|---|
| REQ-SVC-010 | Stage Trading Service behind a dedicated design, interop, Native Image, and security gate. | draft | corba-trading-service, corba-services-core | TRADE-10 |
| REQ-SVC-020 | Stage Event Service behind a dedicated design, interop, Native Image, and security gate. | draft | corba-event-service, corba-services-core | EVNT-12 |
| REQ-SVC-030 | Stage Notification Service behind a dedicated design, interop, Native Image, and security gate. | draft | corba-notification-service, corba-services-core | NOT-11 |
| REQ-SVC-040 | Stage Transaction Service / OTS behind a dedicated design, interop, Native Image, and security gate. | draft | corba-transaction-service, corba-services-core | TRANS-14 |
| REQ-SVC-050 | Stage Security Service / CSIv2 behind a dedicated design, interop, Native Image, and security gate. | draft | corba-security-service, corba-services-core | SEC-18, CORBA-IOP-SECURITY |
| REQ-SVC-060 | Stage Time Service behind a dedicated design, interop, Native Image, and security gate. | draft | corba-time-service, corba-services-core | TIME-11 |

## Gate Rules

- A service requirement does not authorize source, generated artifact, protocol,
  dependency, or public API changes until its human-gated roadmap task is
  approved and split into implementation tasks.
- Each approved service must record its supported specification subset,
  unsupported clauses, compatibility posture, interop scenarios, Native Image
  metadata policy, and hostile-input/security review before implementation.
- `modules/corba-services-core` may hold shared service planning concepts only
  after approval; it must not become a hidden implementation module for
  unapproved services.
