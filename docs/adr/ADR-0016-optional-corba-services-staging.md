# ADR-0016: Optional CORBA Services Staging

Status: accepted
Date: 2026-05-31
Decision owner: Maintainers

Approved: 2026-05-31 by G8 optional services look-back approval in the project thread.

## Context

G6-D20 split Trading, Event, Notification, Transaction, Security, and Time
Services into separate human-gated roadmap tasks. The repository now has a
complete non-optional CORBA 1.0 baseline, durable ORB/POA identity, persistent
Naming, and local durable routing evidence. Optional services can therefore be
designed as explicit post-1.0 extensions without weakening the completed
baseline or silently adding runtime behavior.

## Decision

Approve a staged optional-services design program. Each optional service gets a
dedicated ADR and a blocked follow-on task group before source, generated API,
protocol, dependency, or public artifact changes begin.

The shared rules are:

- `modules/corba-services-core` may hold shared service abstractions only after
  an implementation task names the exact shared contract.
- Service modules own their service-specific APIs, models, diagnostics, local
  behavior, Native Image smoke entrypoints, and interop scenario metadata.
- Implementations must remain generated-code-first and Native Image friendly.
- No optional service may use reflection-driven invocation, runtime bytecode
  generation, dynamic proxies, classpath scanning, Java serialization for
  marshaling or persistence, `Unsafe`, `sun.*`, or `jdk.internal.*`.
- Live peer claims require clean-room scenario design and maintainer-approved
  peer/cache/container prerequisites.
- Reference ORBs remain black-box interoperability peers, not source material.

The suggested implementation order is Time, Event, Notification, Trading,
Transaction/OTS, then Security/CSIv2.

## Consequences

- G8 design gates may be completed as design approvals without implementing
  runtime behavior.
- Follow-on task groups remain blocked until maintainers explicitly promote a
  service implementation slice.
- Service requirements, conformance rows, verification docs, and module READMEs
  must track the accepted staging posture.

## Alternatives considered

- Implement optional services opportunistically in existing runtime modules:
  rejected because it would bypass service-specific interop, Native Image, and
  security reviews.
- Defer all optional services indefinitely: rejected because post-1.0 planning
  now needs a concrete design backlog.
- Build one monolithic optional-services module: rejected because each OMG
  service has distinct APIs, security risks, and interoperability scenarios.

## Specification references

- TRADE-10
- EVNT-12
- NOT-11
- TRANS-14
- SEC-18
- TIME-11
- CORBA-IOP-SECURITY

## Requirements affected

- REQ-SVC-010 through REQ-SVC-060
- REQ-NATIVE-002
- REQ-INTEROP-009
- REQ-SEC-006
- REQ-DOC-006

## Build/test impact

Documentation-only design approval. Follow-on implementation tasks must add
service-specific unit, Native Image, and interop validation.

## Native-image impact

Optional services inherit ADR-0010. Metadata must be deterministic and explicit;
closed-world unsafe mechanisms are forbidden unless a later ADR grants a narrow
waiver.

## Interop impact

No live peer claims are made by this ADR. Peer scenarios must be defined and
approved per service before execution.

## Security impact

Each service ADR records hostile-input boundaries, resource limits, persistence
or recovery assumptions, and failure disclosure before implementation.
