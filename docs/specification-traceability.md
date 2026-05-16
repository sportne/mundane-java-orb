# Specification Traceability

Every requirement must trace to one or more specification references or to an
explicit compatibility/operational requirement.

## Required fields

```yaml
requirementId: REQ-CDR-001
title: Decode primitive CDR values
specReferences:
  - spec: CORBA-3.4-Interoperability
    section: TBD
    note: CDR primitive encoding and alignment
compatibilityProfiles:
  - CORBA_3_4_FULL
  - CORBA_3_3_COMPAT
  - CORBA_3_2_COMPAT
verification:
  - unit
  - golden-wire
  - fuzz
  - native-image
```

## Traceability levels

| Level | Meaning |
|---|---|
| Requirement | What must be true. |
| Design | How the requirement is satisfied architecturally. |
| Test | How the behavior is verified. |
| Conformance matrix | Whether the spec feature is implemented, tested, interop-tested, and native-tested. |
| Implementation task | The narrow agent-executable work item. |

## Rule

A feature is not complete until all traceability levels are updated.
