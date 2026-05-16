# ADR: Native Image as first-class validation target

Status: accepted

## Context

This project is large enough that implementation must be governed by explicit
decisions before coding begins.

## Decision

Validate JVM and Native Image behavior across OpenJDK and GraalVM Java 21/25 matrices.

## Consequences

- Coding-agent work can be scoped and audited.
- Build and architecture gates become part of the project contract.
- Future changes must update this ADR or supersede it.

## Requirements affected

- REQ-NATIVE-001 through REQ-NATIVE-005
- REQ-BUILD-004 and REQ-BUILD-005
- REQ-NFR-001 through REQ-NFR-003

## Specification references

Operational runtime-compatibility decision. Feature-specific Native Image tasks
must cite the relevant CORBA, IDL, language-mapping, or Naming reference from
`docs/specification-traceability.md`.
