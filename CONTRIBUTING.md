# Contributing

This project uses a gated, design-first process.

## Gates

| Gate | Name | Implementation allowed? |
|---|---|---|
| G0 | Charter and standards gate | No |
| G1 | Requirements gate | No |
| G2 | Architecture gate | No |
| G3 | Verification gate | No |
| G4 | Build infrastructure gate | Infrastructure only |
| G5 | Empty-module validation gate | Infrastructure only |
| G6 | Implementation gate | Yes, task-scoped only |

## Pull request requirements

Every PR must state:

- gate affected;
- requirement IDs, if any;
- ADRs affected;
- docs updated;
- tests run;
- commands run;
- whether the change affects native-image behavior;
- whether the change affects interoperability behavior.

## Implementation restrictions

Implementation PRs are forbidden until G0 through G5 have been approved.
Runtime, protocol, IDL, ORB, POA, service, or compiler behavior must not be added
without approved requirement IDs and a task handoff document.
