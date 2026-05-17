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

`corba-modern-api` owns the generated-code-facing request and dispatcher
contracts. `corba-orb-core` owns local object references, local object identity,
dispatcher registration, lifecycle checks, and shutdown coordination.

This path does not open sockets, construct GIOP messages, use IIOP transport,
invoke POA policy behavior, create dynamic proxies, generate runtime bytecode,
or use reflection for dispatch. Generated-style dispatchers call servants
explicitly using static operation descriptors.
