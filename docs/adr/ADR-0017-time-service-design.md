# ADR-0017: Time Service Design

Status: accepted
Date: 2026-05-31
Decision owner: Maintainers

Approved: 2026-05-31 by G8 optional services look-back approval in the project thread.

## Context

The Time Service is the smallest optional service and provides a useful first
slice for shared service patterns: bounded value models, Naming exposure,
Native Image smoke coverage, and deterministic diagnostics without persistence
or complex peer state.

## Decision

Approve a staged Time Service design in `modules/corba-time-service`.

The supported design subset is:

- immutable universal-time and interval value models with explicit precision,
  inaccuracy, and bound checks;
- caller-configured clock source policy with deterministic diagnostics for
  invalid or unavailable clocks;
- local time and interval query behavior before peer interop claims;
- optional IIOP/Naming exposure after local behavior is covered;
- Native Image smoke coverage for value creation, local query, and bounded
  diagnostics.

Non-goals are clock synchronization protocols, distributed clock correction,
timer event delivery, durable scheduling, and peer compatibility claims before
dedicated interop tasks.

## Consequences

Time Service becomes the first recommended optional-service implementation
group. It should establish reusable service diagnostics and Native Image smoke
patterns without moving behavior into `corba-services-core` prematurely.

## Alternatives considered

- Start with Event or Trading Service: rejected because both require more
  complex peer and resource-policy design.
- Use global system time directly everywhere: rejected because tests and Native
  Image smoke need deterministic caller-configured clock behavior.

## Specification references

- TIME-11

## Requirements affected

- REQ-SVC-060
- REQ-NATIVE-002
- REQ-SEC-006
- REQ-DOC-006

## Build/test impact

Follow-on tasks must add focused unit tests, local IIOP/Naming tests if exposed,
Native Image smoke coverage, and structured interop metadata.

## Native-image impact

No reflection, service loading, dynamic proxy, runtime bytecode generation, or
process execution is allowed.

## Interop impact

No live peer claim is accepted yet. Future scenarios should cover peer clients
querying our Time Service and our clients querying peer Time Service endpoints
only after metadata and missing-prerequisite reports are defined.

## Security impact

Clock-source trust, overflow, precision loss, and stale clock diagnostics must
be bounded and deterministic.
