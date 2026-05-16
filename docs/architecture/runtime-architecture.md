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
