# corba-idl-semantics

IDL semantic analyzer, symbol table, type checking, inheritance checks, and constant evaluation.

## Current status

G6-140 implements the first semantic-analysis slice for the minimal IDL parser
subset:

- deterministic symbol models for modules, interfaces, operations, attributes,
  structs, fields, enums, enumerators, exceptions, constants, and parameters;
- scoped and relative name resolution for builtin and user-defined types;
- duplicate-name diagnostics within semantic scopes;
- declaration-order validation for constant references and `raises(...)`
  exception targets;
- simple constant evaluation for integer, floating, boolean, character, string,
  and enum constants.

Out of scope: repository IDs, Java mapping, code generation, CDR/GIOP/IIOP,
ORB runtime behavior, reflection, dynamic scanning, and parser-rejected IDL
constructs such as typedefs, unions, valuetypes, arrays, sequences, pragmas, and
inheritance.

Native Image validation:

- `./gradlew :modules:corba-idl-semantics:nativeIdlSemanticsSmoke` builds and
  runs a GraalVM Native Image smoke executable for parser-to-semantics behavior
  when `native-image` is available through `NATIVE_IMAGE`, `JAVA_HOME`, a
  SDKMAN GraalVM candidate, or final fallback `PATH` lookup. Prefer setting
  `JAVA_HOME` to the SDKMAN GraalVM candidate and prepending `$JAVA_HOME/bin`
  to `PATH` instead of assuming the shell already exposes GraalVM.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
