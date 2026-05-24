# corba-giop

GIOP message model, parser, writer, service contexts, and fragmentation.

## Current status

G10-040 closes the local peer-facing GIOP wire syntax needed before ORB/POA
dispatch. The module exposes a bounded GIOP 1.2 message model plus in-memory
reader/writer support for request, reply, cancel request, locate request,
locate reply, close connection, message error, and fragment messages.

This slice is syntax-only. It supports KeyAddr, ProfileAddr, and ReferenceAddr
target addresses, deterministic user/system exception reply bodies, code-set
service-context helpers, and bounded local fragment assembly. It does not open
sockets, dispatch ORB calls, perform POA lookup, execute live peer interop, or
interpret runtime object semantics.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
