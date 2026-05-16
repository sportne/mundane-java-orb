# ADR: Standards baseline and specification traceability

Status: accepted

## Context

This project is large enough that implementation must be governed by explicit
decisions before coding begins.

## Decision

CORBA 3.4 is the primary normative baseline; CORBA 3.3, CORBA 3.2, and legacy Java/CORBA behavior are compatibility profiles.

## Consequences

- Coding-agent work can be scoped and audited.
- Build and architecture gates become part of the project contract.
- Future changes must update this ADR or supersede it.

## Requirements affected

- REQ-IDL-001 through REQ-IDL-003
- REQ-IDLJ-001 through REQ-IDLJ-004
- REQ-CDR-001
- REQ-GIOP-001
- REQ-IIOP-001 and REQ-IIOP-002
- REQ-IOR-001 and REQ-IOR-002
- REQ-ORB-001
- REQ-POA-001 and REQ-POA-002
- REQ-DYN-001
- REQ-INT-001
- REQ-NAM-001
- REQ-RMI-001

## Specification references

- CORBA-IF
- CORBA-IOP
- IDL-42
- I2JAV-13
- JAV2I-14
- NAM-13
