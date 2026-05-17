# corba-idl-ast

IDL AST and normalized syntax model.

## Current status

G6 IDL compiler foundation work has started with immutable syntax-only AST
nodes for the minimal parser slice.

This module records parser output for modules, interfaces, operations,
attributes, structs, enums, exceptions, constants, type references, fields,
parameters, and unevaluated constant expressions.

This module does not implement semantic analysis, type checking, repository ID
derivation, Java mapping, code generation, ORB/runtime behavior, or protocol
behavior.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
