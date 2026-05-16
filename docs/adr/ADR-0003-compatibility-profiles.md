# ADR: Compatibility profiles

Status: accepted

## Context

This project is large enough that implementation must be governed by explicit
decisions before coding begins.

## Decision

Use one implementation with explicit compatibility profiles rather than separate implementations per CORBA version.

## Consequences

- Coding-agent work can be scoped and audited.
- Build and architecture gates become part of the project contract.
- Future changes must update this ADR or supersede it.

## Requirements affected

- REQ-NFR-006
- REQ-INTEROP-001 through REQ-INTEROP-009
- REQ-RMI-001

## Specification references

- CORBA-IOP-ARCH
- CORBA-IOP-GIOP
- CORBA-IOP-IIOP
- I2JAV-13
- JAV2I-14-RMI-IDL
