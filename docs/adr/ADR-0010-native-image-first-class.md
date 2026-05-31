# ADR: Native Image as first-class validation target

Status: accepted

## Context

This project is large enough that implementation must be governed by explicit
decisions before coding begins.

## Decision

Validate JVM and Native Image behavior across OpenJDK and GraalVM Java 21/25
matrices. Optional CORBA services inherit this policy: service implementations
must use explicit runtime objects and deterministic metadata, not reflection
metadata, classpath scanning, service-loader discovery, dynamic proxies, Java
serialization metadata, runtime bytecode generation, `Unsafe`, `sun.*`, or
`jdk.internal.*`.

## Consequences

- Coding-agent work can be scoped and audited.
- Build and architecture gates become part of the project contract.
- Future changes must update this ADR or supersede it.
- Optional service ADRs may add stricter service-specific Native Image smoke
  expectations, but they do not weaken this baseline.

## Requirements affected

- REQ-NATIVE-001 through REQ-NATIVE-005
- REQ-BUILD-004 and REQ-BUILD-005
- REQ-NFR-001 through REQ-NFR-003

## Specification references

Operational runtime-compatibility decision. Feature-specific Native Image tasks
must cite the relevant CORBA, IDL, language-mapping, Naming, or optional-service
reference from `docs/specification-traceability.md`.
