# ADR: Multi-artifact publication model

Status: accepted

## Context

This project is large enough that implementation must be governed by explicit
decisions before coding begins.

## Decision

Publish independently usable artifacts plus a BOM.

## Consequences

- Coding-agent work can be scoped and audited.
- Build and architecture gates become part of the project contract.
- Future changes must update this ADR or supersede it.

## Requirements affected

- REQ-BUILD-010
- REQ-NFR-006
- REQ-NFR-008

## Specification references

Packaging decision supporting the CORBA compatibility and modern API split; no
direct OMG specification clause.
