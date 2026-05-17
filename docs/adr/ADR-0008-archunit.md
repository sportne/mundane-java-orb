# ADR: ArchUnit architecture enforcement

Status: accepted

## Context

This project is large enough that implementation must be governed by explicit
decisions before coding begins.

## Decision

Use ArchUnit as a build-enforced architectural boundary tool.
The enforceable rule catalog is maintained in
`docs/architecture/architecture-rule-catalog.md` and implemented primarily by
the central `modules/corba-architecture-tests` module.

## Consequences

- Coding-agent work can be scoped and audited.
- Build and architecture gates become part of the project contract.
- Architecture rules are grouped into project-specific, GraalVM Native Image,
  and general Java baseline categories.
- Future changes must update this ADR or supersede it.

## Requirements affected

- REQ-BUILD-007
- REQ-NFR-003

## Specification references

Architecture-enforcement decision; no direct OMG specification clause.
