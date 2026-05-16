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

## G6 tightening policy

Empty modules continue to skip coverage verification until they contain compiled
production classes. Once a module has production classes, `qualityGate` enforces
the baseline individual-source policy: at least 80% line coverage and 70% branch
coverage at the bundle level.

Higher domain thresholds from the table above are introduced by the feature
tasks that add CDR/GIOP/IIOP/IOR, IDL, ORB, POA, or security-sensitive behavior.

## Exclusions

Allowed exclusions:

- generated code;
- `module-info.java`;
- `package-info.java`;
- pure constants classes;
- exception classes with no behavior;
- test fixtures.

Any exclusion requires documentation.
