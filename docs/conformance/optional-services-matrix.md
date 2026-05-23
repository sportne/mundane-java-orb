# Optional Services Conformance Matrix

G6-D20 records service-gate traceability only. These rows do not claim
implementation, local compatibility, Native Image execution, or live peer
interop.

| Service behavior | Clause / section | Requirement IDs | Status | Test IDs | Notes |
|---|---|---|---|---|---|
| Trading Service | TRADE-10 | REQ-SVC-010, REQ-SVC-001 | deferred | unassigned | Split into `G8-D10-TRADING-SERVICE-DESIGN-GATE`. No trader, offer repository, service type repository, import/export, or constraint behavior is implemented. |
| Event Service | EVNT-12 | REQ-SVC-020, REQ-SVC-001 | deferred | unassigned | Split into `G8-D20-EVENT-SERVICE-DESIGN-GATE`. No event channel, push/pull supplier, push/pull consumer, or channel lifecycle behavior is implemented. |
| Notification Service | NOT-11 | REQ-SVC-030, REQ-SVC-001 | deferred | unassigned | Split into `G8-D30-NOTIFICATION-SERVICE-DESIGN-GATE`. No notification channel, filtering, QoS/admin, or Event Service compatibility behavior is implemented. |
| Transaction Service / OTS | TRANS-14 | REQ-SVC-040, REQ-SVC-001 | deferred | unassigned | Split into `G8-D40-TRANSACTION-SERVICE-DESIGN-GATE`. No coordinator, resource, propagation, recovery, or transaction context behavior is implemented. |
| Security Service / CSIv2 | SEC-18; CORBA-IOP-SECURITY | REQ-SVC-050, REQ-SVC-001 | deferred | unassigned | Split into `G8-D50-SECURITY-SERVICE-DESIGN-GATE`. No credential, policy, access-decision, audit, secure invocation, or CSIv2 behavior is implemented. |
| Time Service | TIME-11 | REQ-SVC-060, REQ-SVC-001 | deferred | unassigned | Split into `G8-D60-TIME-SERVICE-DESIGN-GATE`. No universal time, time interval, timer event, or clock synchronization behavior is implemented. |
