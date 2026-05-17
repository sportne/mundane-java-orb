# IDL Fixture Corpus

This directory contains IDL input fixtures for verification tasks. Fixture paths
are relative paths consumed through `modules/corba-testkit`; absolute paths and
`..` traversal segments are not valid fixture metadata.

`hello/hello.idl` is the first real shared IDL fixture. G6-210 uses it to drive
the test-only parser, semantics, mapper, and source-generation pipeline and to
validate the same input through `corba-idlj validate --quiet`.
