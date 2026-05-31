# ADR-0019: Notification Service Design

Status: accepted
Date: 2026-05-31
Decision owner: Maintainers

Approved: 2026-05-31 by G8 optional services look-back approval in the project thread.

## Context

Notification Service extends Event Service concepts with structured events,
filtering, QoS, and administrative policy. It has higher parsing and resource
risk than Event Service, so it should follow an Event Service foundation.

## Decision

Approve a staged Notification Service design in
`modules/corba-notification-service`.

The supported design subset is:

- notification channel lifecycle aligned with Event Service compatibility;
- structured event model and bounded event payload validation;
- explicit filter model with bounded expression evaluation;
- QoS and administrative policy validation with deterministic diagnostics;
- local delivery before IIOP and live peer claims;
- Native Image smoke coverage for channel creation, filter validation, QoS
  rejection, and local delivery.

Non-goals are unbounded filter languages, durable subscriptions, persistent
event storage, transaction integration, and live peer claims before dedicated
interop tasks.

## Consequences

Notification Service is the third recommended optional-service group and should
reuse Event Service channel patterns while keeping filtering and QoS in its own
module.

## Alternatives considered

- Merge Notification into Event Service: rejected because filtering and QoS
  policy have different security and interoperability risks.
- Evaluate filter expressions with a general-purpose scripting engine: rejected
  because it violates bounded Native Image and security requirements.

## Specification references

- NOT-11
- EVNT-12

## Requirements affected

- REQ-SVC-030
- REQ-SVC-020
- REQ-NATIVE-002
- REQ-INTEROP-009
- REQ-SEC-006
- REQ-DOC-006

## Build/test impact

Follow-on tasks must add structured-event tests, filter limit tests, QoS/admin
policy tests, Native Image smoke, and structured interop metadata.

## Native-image impact

Filtering must use explicit parsers/evaluators and static descriptors, not
reflection, scripting, dynamic proxies, or runtime bytecode generation.

## Interop impact

No live peer claim is accepted yet. Future peer scenarios should cover
structured events, filters, QoS rejection, and Event Service compatibility.

## Security impact

Filter parsing/evaluation, QoS limits, administrative limits, and failure
messages must be bounded and deterministic.
