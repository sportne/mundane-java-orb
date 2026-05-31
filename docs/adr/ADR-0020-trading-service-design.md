# ADR-0020: Trading Service Design

Status: accepted
Date: 2026-05-31
Decision owner: Maintainers

Approved: 2026-05-31 by G8 optional services look-back approval in the project thread.

## Context

Trading Service adds service type metadata, offer repositories, constraint
queries, and import/export behavior. Its constraint language and repository
limits need a dedicated design before implementation.

## Decision

Approve a staged Trading Service design in `modules/corba-trading-service`.

The supported design subset is:

- service type repository metadata with bounded names and property models;
- offer repository CRUD with explicit resource limits;
- deterministic constraint parser and evaluator for a narrow approved subset;
- local query behavior before import/export and IIOP exposure;
- optional Naming exposure for trader references after local behavior is
  covered;
- Native Image smoke coverage for type registration, offer registration,
  constraint rejection, and local query.

Non-goals are unbounded constraint expressions, remote federated trader graphs,
durable offer persistence, transaction integration, and live peer claims before
dedicated interop tasks.

## Consequences

Trading Service is the fourth recommended optional-service group because it
benefits from earlier service diagnostics and Native Image patterns but is less
coupled to Event/Notification.

## Alternatives considered

- Implement full constraint language immediately: rejected because hostile
  inputs and expression limits need staged validation.
- Store offers durably in the first slice: rejected because operational
  retention and recovery policy need a later persistence task.

## Specification references

- TRADE-10

## Requirements affected

- REQ-SVC-010
- REQ-NATIVE-002
- REQ-INTEROP-009
- REQ-SEC-006
- REQ-DOC-006

## Build/test impact

Follow-on tasks must add service type tests, offer repository tests,
constraint-parser hostile-input tests, Native Image smoke, and structured
interop metadata.

## Native-image impact

Constraint parsing/evaluation must be explicit and closed-world friendly. No
scripting engines, reflection, or runtime code generation are allowed.

## Interop impact

No live peer claim is accepted yet. Future scenarios should cover trader
registration, lookup, query, and import/export only after report metadata exists.

## Security impact

Constraint parsing, offer counts, property values, query cost, and import/export
fan-out must be bounded.
