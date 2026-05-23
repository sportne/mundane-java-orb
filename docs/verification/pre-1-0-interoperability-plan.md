# Pre-1.0 Interoperability Plan

The pre-1.0 interoperability program is tracked by the G10 roadmap tasks. It is
not a release approval by itself. It defines the non-optional implementation and
verification work that must complete before
`G10-120-PRE-1.0-FULL-INTEROP-EXECUTION` can run.

## Release Bar

Version `1.0.0` must not be declared until the non-optional CORBA, IDL,
IDL-to-Java, RMI-IIOP, Native Image, and live peer interoperability tasks in
G10 are complete. Optional CORBA Services remain deferred behind the existing G8
human gates and are not part of the 1.0.0 interop release bar.

## Task Sequence

G10 is ordered to close prerequisites before live peer execution:

1. IDL 4.2 grammar closure.
2. IDL-to-Java legacy mapping closure.
3. OMG compatibility API surface.
4. CDR, GIOP, IIOP, and IOR wire closure.
5. Network ORB/POA dispatch.
6. Network Naming and object URL behavior.
7. DynamicAny, DII, DSI, Any, TypeCode, and Interface Repository wire closure.
8. Portable Interceptors.
9. RMI-IIOP compatibility closure.
10. Native Image interop binaries.
11. Real peer harness closure.
12. Full pre-1.0 live interop execution.

The final execution task remains blocked until all prerequisite G10 tasks are
complete.

## Peer And Scenario Policy

Approved peers remain JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB,
and ACE/TAO. Peer use stays black-box only: no vendored peer source, committed
peer binaries, source-derived implementation logic, or committed live outputs.

The full matrix covers non-optional implemented behavior across our JVM and
Native Image client/server lanes, peer client/server lanes, Naming, IOR/object
URL, GIOP/IIOP, IDL-to-Java, DynamicAny/DII/DSI, Portable Interceptor, and
RMI-IIOP scenarios. Optional services are excluded until service-specific human
gates approve separate scenarios.

## Execution Prerequisites

Live interop execution requires:

- approved external peer cache entries verified by SHA-256;
- digest-pinned Java and native base images;
- Docker or Podman;
- Native Image when native lanes are required;
- structured reports for pass, fail, skipped, and infrastructure outcomes.

Cache preparation must be opt-in and write only to an untracked external cache.
No live fetch is part of the default local validation path.

## Acceptance

`G10-120` passes only when each non-optional scenario either passes or has a
maintainer-approved clean-room classification of `peer-bug`, `spec-ambiguity`,
or `profile-mismatch`. Any `our-bug` finding blocks `1.0.0` until fixed and
rerun. Infrastructure failures block release unless maintainers explicitly
accept them as environment limitations outside the release evidence set.
