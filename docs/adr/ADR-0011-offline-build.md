# ADR: Offline build support

Status: accepted

## Context

This project is large enough that implementation must be governed by explicit
decisions before coding begins.

## Decision

Support offline builds with a supplied local Maven repository, dependency locking, and dependency verification.

## Consequences

- Coding-agent work can be scoped and audited.
- Build and architecture gates become part of the project contract.
- Future changes must update this ADR or supersede it.

## Requirements affected

- REQ-OFFLINE-001 through REQ-OFFLINE-006
- REQ-BUILD-002
- REQ-NFR-005

## Specification references

Build-reproducibility decision; no direct OMG specification clause.
