# Runtime Architecture

## ORB core responsibilities

- lifecycle;
- configuration;
- initial references;
- object reference abstraction;
- invocation pipeline;
- interceptor pipeline;
- timeout policy;
- exception mapping;
- shutdown coordination.

## Server path

```text
IIOP listener
  -> GIOP frame decoder
  -> object key lookup
  -> POA / servant resolution
  -> generated skeleton dispatcher
  -> CDR reply encoder
  -> GIOP reply
```

## Client path

```text
generated stub
  -> operation descriptor
  -> CDR request body
  -> GIOP request
  -> IIOP connection
  -> reply correlation
  -> CDR reply body
  -> typed value or exception
```

## Local invocation path

The first G6 local invocation slice is intentionally in-process only:

```text
generated client
  -> LocalOrb.invoke
  -> LocalInvocationDispatcher
  -> generated-style servant
```

Exceptions follow the same in-process path and are normalized before they leave
ORB core:

```text
generated client
  -> LocalOrb.invoke
  -> LocalInvocationDispatcher
  -> generated-style servant
  -> local exception mapper
  -> typed user wrapper or CORBA system exception
```

`corba-modern-api` owns the generated-code-facing request and dispatcher
contracts. `corba-orb-core` owns local object references, local object identity,
dispatcher registration, typed local initial references, lifecycle checks,
exception mapping, and shutdown coordination. `corba-omg-api` owns the minimal
`org.omg.CORBA` exception compatibility surface used by this local slice.

## Local Naming Service Path

G6-810 adds a local Naming Service path over in-process initial references:

```text
LocalNamingService.install
  -> LocalOrb.registerInitialReference("NameService")
  -> NamingContext bind / resolve / list / destroy
  -> CorbanameResolver for corbaname:rir:#name
```

This path is an in-memory local JVM slice. It does not contact remote IIOP
addresses, open a Naming Service transport endpoint, persist naming databases,
discover services dynamically, or expose legacy CosNaming compatibility APIs.

This path does not open sockets, construct GIOP messages, use IIOP transport,
invoke POA policy behavior, create dynamic proxies, generate runtime bytecode,
use reflection for dispatch, or marshal exceptions as CDR reply bodies.
Generated-style dispatchers call servants explicitly using static operation
descriptors.

## Local RMI-IIOP Adapter Path

G7-070 adds generated RMI-IIOP adapters over the same in-process ORB and POA
contracts:

```text
generated RMI stub
  -> LocalOrb.invoke
  -> Poa dispatch
  -> generated RMI tie or skeleton
  -> servant
```

This path exposes generated static binding descriptors, operation descriptors,
local stubs, ties, and skeleton activation helpers for the approved RMI-IIOP
slice. It does not open sockets, construct GIOP request/reply frames, use IIOP
transport, discover classes dynamically, create proxies, invoke through
reflection, or marshal values through Java serialization.
