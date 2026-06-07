# corba-time-service

Time Service implementation module.

## Current status

Design accepted by ADR-0017. `G8-100-TIME-SERVICE-TASK-GROUP` implements the
first local slice: TimeBase-compatible UTC and interval values, caller-provided
clock policy, deterministic local query diagnostics, and Native Image smoke
coverage. `G8-110-TIME-SERVICE-IIOP-NAMING-EXPOSURE` adds descriptor-backed
loopback IIOP dispatch, TimeBase field codecs, client/server helpers, and
optional network Naming registration.

`G8-120-TIME-SERVICE-INTEROP-METADATA` adds the approved-peer `time-service`
manifest scenario and dry-run missing-prerequisite reporting.
`G8-140-TIME-SERVICE-CONFORMANCE-CLOSURE` records approved live peer evidence
for JacORB, GlassFish CORBA ORB, and JBoss OpenJDK ORB clients invoking our JVM
and Native Image Time Service servers. ACE/TAO and reverse peer-server Time
Service lanes are recorded as unsupported for the value-returning subset.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
