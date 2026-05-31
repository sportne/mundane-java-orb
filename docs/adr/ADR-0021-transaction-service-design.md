# ADR-0021: Transaction Service And OTS Design

Status: accepted
Date: 2026-05-31
Decision owner: Maintainers

Approved: 2026-05-31 by G8 optional services look-back approval in the project thread.

## Context

Transaction Service / OTS has coordinator, resource, propagation, timeout, and
recovery concerns. It can affect request contexts and durable operational state,
so it should follow simpler optional services.

## Decision

Approve a staged Transaction Service / OTS design in
`modules/corba-transaction-service`.

The supported design subset is:

- local transaction coordinator and resource model with explicit timeout
  policy;
- bounded propagation metadata design before IIOP request-context integration;
- deterministic rollback, heuristic, stale-resource, and timeout diagnostics;
- recovery design recorded before any durable transaction log is implemented;
- Native Image smoke coverage for coordinator/resource state transitions and
  hostile timeout inputs.

Non-goals are XA integration, durable recovery logs, distributed two-phase
commit over peers, Security Service integration, and live peer claims before
dedicated interop tasks.

## Consequences

Transaction Service is the fifth recommended optional-service group because it
requires more operational policy than Time, Event, Notification, or Trading.

## Alternatives considered

- Implement durable recovery first: rejected because log format, retention, and
  operator policy need separate approval.
- Couple transactions directly into IIOP before local behavior: rejected because
  propagation semantics need local tests and diagnostics first.

## Specification references

- TRANS-14

## Requirements affected

- REQ-SVC-040
- REQ-NATIVE-002
- REQ-INTEROP-009
- REQ-SEC-006
- REQ-DOC-006

## Build/test impact

Follow-on tasks must add coordinator/resource tests, timeout tests, propagation
metadata tests if exposed, Native Image smoke, and structured interop metadata.

## Native-image impact

Resource and coordinator factories must be explicit. No reflective resource
discovery, Java serialization, dynamic proxies, or runtime bytecode generation
is allowed.

## Interop impact

No live peer claim is accepted yet. Future scenarios should cover coordinator
and resource behavior only after local propagation metadata is stable.

## Security impact

Timeouts, resource cleanup, stale propagation contexts, recovery assumptions,
and failure disclosure must be bounded and deterministic.
