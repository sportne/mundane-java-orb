# ADR-0018: Event Service Design

Status: accepted
Date: 2026-05-31
Decision owner: Maintainers

Approved: 2026-05-31 by G8 optional services look-back approval in the project thread.

## Context

The Event Service introduces channel lifecycle, supplier/consumer connections,
push and pull delivery, and bounded fan-out. It should precede Notification
Service because Notification builds on event-channel concepts.

## Decision

Approve a staged Event Service design in `modules/corba-event-service`.

The supported design subset is:

- explicit event channel lifecycle with bounded supplier and consumer counts;
- push supplier/consumer and pull supplier/consumer surfaces staged separately;
- local in-JVM delivery before IIOP exposure;
- deterministic backpressure, disconnect, and failed-consumer diagnostics;
- optional Naming exposure for event channels after local behavior is covered;
- Native Image smoke coverage for channel creation, connection, delivery, and
  bounded failure paths.

Non-goals are persistent queues, durable subscriptions, transaction integration,
Notification QoS/filtering, and live peer claims before interop tasks.

## Consequences

Event Service is the second recommended optional-service group and should
create reusable channel lifecycle patterns for Notification Service.

## Alternatives considered

- Implement Notification Service first: rejected because Notification depends
  on Event Service compatibility concepts.
- Add durable queues in the first slice: rejected because persistence and
  delivery guarantees need separate operational design.

## Specification references

- EVNT-12

## Requirements affected

- REQ-SVC-020
- REQ-NATIVE-002
- REQ-INTEROP-009
- REQ-SEC-006
- REQ-DOC-006

## Build/test impact

Follow-on tasks must add local channel tests, bounded backpressure tests,
IIOP/Naming tests if exposed, Native Image smoke, and structured interop
metadata.

## Native-image impact

Suppliers, consumers, and dispatch tables must be explicit runtime objects.
No reflective callback discovery or dynamic proxy generation is allowed.

## Interop impact

No live peer claim is accepted yet. Future scenarios should cover push and pull
channel interoperability only after missing-prerequisite reports exist.

## Security impact

Channel fan-out, queue depth, backpressure, connection lifecycle, and failure
disclosure must be bounded.
