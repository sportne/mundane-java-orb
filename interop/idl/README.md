# IDL Fixture Corpus

This directory contains IDL input fixtures for verification tasks. Fixture paths
are relative paths consumed through `modules/corba-testkit`; absolute paths and
`..` traversal segments are not valid fixture metadata.

`hello/hello.idl` is the first real shared IDL fixture. G6-210 uses it to drive
the test-only parser, semantics, mapper, and source-generation pipeline and to
validate the same input through `corba-idlj validate --quiet`.

`g12-wide/` contains the post-1.0 broad IDL feature corpus. G12-050 uses these
fixtures for local parser, semantic, IDL-to-Java mapping, generated-source
compilation, JVM interop report, and Native Image report lanes:

- `CoreTypes.idl` covers bounded sequences, typedefs, enums, structs, unions,
  exceptions, attributes, holder-using operations, raises clauses, and operation
  contexts.
- `RepositoryPragmas.idl` covers repository prefixes, native declarations, value
  boxes, abstract valuetype bases, supported interfaces, and type prefixes.
- `ValueTypes.idl` covers native handles, abstract interfaces with operations,
  value boxes, valuetype inheritance, state members, factories, and supported
  interface operations.
- `UnsupportedCustomValue.idl` is intentionally parser/semantic-valid but
  rejected by the current IDL-to-Java mapping because custom value marshaling is
  not implemented.
