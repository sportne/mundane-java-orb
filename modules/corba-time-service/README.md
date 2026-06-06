# corba-time-service

Time Service implementation module.

## Current status

Design accepted by ADR-0017. `G8-100-TIME-SERVICE-TASK-GROUP` implements the
first local slice: TimeBase-compatible UTC and interval values, caller-provided
clock policy, deterministic local query diagnostics, and Native Image smoke
coverage.

IIOP/Naming exposure, structured interop metadata, live peer execution, and
conformance closure remain staged in follow-on G8 Time Service tasks.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
