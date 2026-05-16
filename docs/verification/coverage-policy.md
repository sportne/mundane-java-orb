# Coverage Policy

Coverage is necessary but not sufficient. Protocol correctness also requires
spec tests, golden-wire tests, fuzz tests, and interop tests.

## Target thresholds after implementation begins

| Area | Line coverage | Branch coverage |
|---|---:|---:|
| Whole project, excluding generated code | 85% | 75% |
| Individual source file default | 80% | 70% |
| CDR / GIOP / IIOP / IOR | 92% | 88% |
| IDL parser / semantics | 90% | 85% |
| ORB core / POA | 88% | 82% |
| Security-sensitive parsing paths | 95% | 90% |

## Scaffold policy

The scaffold currently sets a permissive JaCoCo threshold so empty modules can be
validated. Raising the threshold to the table above is a G5-to-G6 acceptance item.

## Exclusions

Allowed exclusions:

- generated code;
- `module-info.java`;
- `package-info.java`;
- pure constants classes;
- exception classes with no behavior;
- test fixtures.

Any exclusion requires documentation.
