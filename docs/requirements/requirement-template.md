# Requirement Template

```yaml
requirementId: REQ-AREA-000
title: Short requirement title
status: draft | approved | implemented | tested | interop-tested | native-tested
summary: One paragraph summary.
priority: must | should | may
normativeReferences:
  - spec: CORBA-3.4-Interoperability
    section: "<section>"
    url: https://www.omg.org/spec/CORBA/3.4/Interoperability/PDF
compatibilityProfiles:
  - CORBA_3_4_FULL
targetModules:
  - modules/<target-module>
verification:
  - unit
  - golden-wire
  - integration
  - native-image
interopPeers:
  - jacorb
  - glassfish-orb
securityImplications: "<impact or none>"
nativeImageImplications: "<impact or none>"
documentationRequired:
  - architecture doc
  - conformance matrix
  - package-info.java
acceptanceNotes: "<objective acceptance notes>"
```
