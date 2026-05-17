# corba-testkit

Reusable test fixtures, IDL corpus tools, and golden-wire utilities.

## Current status

G6 golden fixture foundation behavior is implemented here. The module provides
test infrastructure for:

- loading fixtures from an explicit root with traversal-safe relative paths;
- reading UTF-8 text and bytes;
- normalizing text fixture line endings and a single leading UTF-8 BOM;
- comparing golden text and byte fixtures with deterministic failure messages;
- recording fixture metadata for IDL, golden-source, and golden-wire assets.

This module does not implement CORBA runtime, protocol, IDL compiler, code
generation, ORB, POA, service, or external peer interop behavior.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
