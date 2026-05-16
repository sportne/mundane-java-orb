# Implementation Task Template

```yaml
taskId: TASK-0000
title: Short task title
gate: G6
requirementIds:
  - REQ-AREA-000
specReferences:
  - spec: CORBA-3.4-Interoperability
    section: TBD
compatibilityProfiles:
  - CORBA_3_4_FULL
targetModule: modules/corba-example
allowedFiles:
  - modules/corba-example/src/main/java/**
  - modules/corba-example/src/test/java/**
forbiddenFiles:
  - build-logic/**
  - gradle/**
  - docs/adr/** unless explicitly requested
externalOrbReferencesAllowed:
  - black-box behavior notes only
expectedTests:
  - unit
  - golden-wire
expectedDocumentationUpdates:
  - docs/conformance/example-matrix.md
  - modules/corba-example/README.md
coverageImpact: Must not lower thresholds.
nativeImageImpact: State whether metadata or class initialization changes are needed.
securityImpact: State bounds and hostile-input implications.
acceptanceCommand: ./gradlew clean check
```
