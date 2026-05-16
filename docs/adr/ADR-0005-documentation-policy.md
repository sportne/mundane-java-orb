# ADR: Documentation policy

Status: accepted

## Context

This project is large enough that implementation must be governed by explicit
decisions before coding begins.

## Decision

Public APIs, protocol behavior, generated code, and design decisions require documentation.

## Consequences

- Coding-agent work can be scoped and audited.
- Build and architecture gates become part of the project contract.
- Future changes must update this ADR or supersede it.

## Requirements affected

- REQ-DOC-001 through REQ-DOC-006

## Specification references

Operational documentation policy; feature documentation must cite the relevant
canonical reference from `docs/specification-traceability.md`.
