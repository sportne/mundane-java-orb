# corba-iiop

IIOP TCP/TLS transport, connection management, request correlation, and timeouts.

## Current status

G6 local loopback TCP and endpoint-local TLS/mTLS behavior has started. The
module exposes a bounded IIOP client/server transport for complete GIOP
request/reply messages, including timeouts, request-id correlation, basic
connection backpressure, idempotent shutdown, and explicit per-endpoint TLS
configuration.

This slice is intentionally local and transport-only. It does not implement
connection pooling, ORB dispatch, POA lookup, Naming Service behavior,
generated stubs or skeletons, exception body marshaling, TLS tagged components
in IORs, CORBA Security Service, or external ORB peer interop.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
