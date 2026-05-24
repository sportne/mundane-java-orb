# corba-interceptors

Portable interceptor request-flow APIs and runtime integration.

## Current status

G10-080 adds bounded, explicit Portable Interceptor registration and request-flow
callbacks for the implemented ORB/IIOP loopback path. Client and server
interceptors run in deterministic order, propagate GIOP service contexts, and
report callback failures with stable diagnostics.

This module does not own CORBA Security Service policy behavior, ORB object
adaptation, POA lookup, transport framing, runtime classpath scanning,
reflection dispatch, dynamic proxies, Java serialization, or service-loader
discovery.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
