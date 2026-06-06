# corba-time-service

Time Service implementation module.

## Current status

Design accepted by ADR-0017. `G8-100-TIME-SERVICE-TASK-GROUP` implements the
first local slice: TimeBase-compatible UTC and interval values, caller-provided
clock policy, deterministic local query diagnostics, and Native Image smoke
coverage. `G8-110-TIME-SERVICE-IIOP-NAMING-EXPOSURE` adds descriptor-backed
loopback IIOP dispatch, TimeBase field codecs, client/server helpers, and
optional network Naming registration.

Structured interop metadata, live peer execution, and conformance closure remain
staged in follow-on G8 Time Service tasks. `G8-120-TIME-SERVICE-INTEROP-METADATA`
adds the approved-peer `time-service` manifest scenario and dry-run
missing-prerequisite reporting, but does not execute live peers.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
