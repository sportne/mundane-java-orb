# corba-iiop

IIOP TCP/TLS transport, connection management, request correlation, and timeouts.

## Current status

G6 local loopback TCP and endpoint-local TLS/mTLS behavior has started. The
module exposes a bounded IIOP client/server transport for complete GIOP
request/reply messages, including timeouts, request-id correlation, basic
connection backpressure, idempotent shutdown, and explicit per-endpoint TLS
configuration. G10-040 adds bounded GIOP fragment-sequence assembly in the
frame reader before messages are returned to the existing request/reply paths.
G8-660 adds Security Service / CSIv2 descriptor-backed service-context and
tagged-component handling in `modules/corba-security-service`; it does not
change this module's transport TLS policy or start live secure peer execution.

This slice is intentionally local and transport-only. It does not implement
connection pooling, ORB dispatch, POA lookup, Naming Service behavior,
generated stubs or skeletons, TLS policy interpretation, CORBA Security
Service, or external ORB peer interop.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
