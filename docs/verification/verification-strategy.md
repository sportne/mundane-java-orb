# Verification Strategy

The project uses layered verification:

1. unit tests;
2. architecture tests;
3. golden-source tests;
4. generated-code compilation tests;
5. golden-wire tests;
6. property tests;
7. negative protocol tests;
8. fuzz tests;
9. local integration tests;
10. external ORB interop tests;
11. Native Image tests;
12. performance and soak tests;
13. offline build tests.

Reusable fixture loading, text normalization, golden-source comparisons, and
golden-wire comparisons follow `fixture-conventions.md`.

A feature is not complete until its conformance matrix row shows implementation,
unit testing, integration testing, interop testing where applicable, and
native-image testing where applicable.

G6 release hardening closure evidence is recorded in
`g6-release-hardening-closure.md`. That closure uses deterministic hostile-input
and bounded-loop smoke tests in the normal unit lane rather than introducing
flaky wall-clock benchmarks or environment-dependent fuzz tasks.
