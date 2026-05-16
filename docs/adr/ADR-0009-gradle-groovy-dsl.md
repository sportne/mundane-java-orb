# ADR: Gradle 9.5.1 Groovy DSL

Status: accepted

## Context

This project is large enough that implementation must be governed by explicit
decisions before coding begins.

## Decision

Use the pinned Gradle 9.5.1 wrapper and Groovy DSL.

## Consequences

- Coding-agent work can be scoped and audited.
- Build and architecture gates become part of the project contract.
- Future changes must update this ADR or supersede it.

## Requirements affected

TBD during G1 completion.

## Specification references

TBD where applicable.
