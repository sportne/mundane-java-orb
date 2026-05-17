# corba-codegen

Deterministic Java source generation and generated metadata support.

## Current status

G6-160 implements the first deterministic Java source renderer for the minimal
IDL-to-Java mapping model.

The renderer emits compile-safe Java source for:

- Java interfaces from IDL interfaces;
- final value classes from IDL structs;
- Java enums from IDL enums;
- checked exception classes from IDL exceptions;
- deterministic constant holder classes for constants declared in each IDL
  scope.

Generated source includes source IDL identity, mapping mode, and compatibility
profile comments. It intentionally avoids `org.omg.*`, ORB runtime APIs, CDR,
repository IDs, helpers, holders, stubs, skeletons, POA classes, reflection, and
dynamic class loading in this slice.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
