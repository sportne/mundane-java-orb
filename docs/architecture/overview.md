# Architecture Overview

The project is split into compatibility-facing APIs, modern APIs, compiler
modules, protocol modules, runtime modules, service modules, native-image
modules, testkits, and interop test infrastructure.

## Principle

Legacy outside, modern inside.

- Compatibility artifacts may expose old CORBA Java shapes.
- Internals shall use generated dispatch, immutable configuration, static
  descriptors, bounded parsers, and explicit runtime state.

## Top-level flow

```text
IDL source
  -> parser / semantics
  -> mapping / codegen
  -> generated stubs, skeletons, codecs, descriptors
  -> ORB invocation pipeline
  -> CDR / GIOP / IIOP
  -> peer ORB or local server
```
