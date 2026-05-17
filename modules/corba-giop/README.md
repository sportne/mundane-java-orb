# corba-giop

GIOP message model, parser, writer, service contexts, and fragmentation.

## Current status

G6 local wire-message behavior has started. The module exposes a bounded GIOP
1.2 message model plus in-memory reader/writer support for request, reply,
cancel request, locate request, locate reply, close connection, message error,
and fragment messages.

This slice is syntax-only. It does not open sockets, start IIOP transport,
dispatch ORB calls, perform POA lookup, interpret service contexts, or implement
full object-reference semantics. Request and locate target addresses support
only KeyAddr; ProfileAddr and ReferenceAddr are rejected until IOR integration.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
