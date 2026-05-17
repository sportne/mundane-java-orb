# Fixture Conventions

Reusable fixtures are loaded through `modules/corba-testkit` helpers. Fixture
paths are always relative to an explicit root directory, must not be absolute,
and must not contain `..` traversal segments.

## Fixture kinds

| Kind | Use |
|---|---|
| `IDL` | IDL input corpus files. |
| `GOLDEN_SOURCE` | Expected generated source text. |
| `GOLDEN_WIRE` | Expected protocol byte sequences. |

## Text comparison

Golden text comparison removes one leading UTF-8 BOM and normalizes CRLF and CR
line endings to LF. It does not trim leading spaces, trailing spaces, or final
newlines.

## Byte comparison

Golden wire comparison is exact byte comparison. Failure messages identify the
first differing offset, expected and actual lengths, and the expected and actual
byte values at that offset.

## Current scope

This convention defines reusable test infrastructure and the shared fixture
layout. G6-210 adds `interop/idl/hello/hello.idl` as the first real IDL fixture
and stores its expected generated Java under the codegen test resources.
Protocol byte vectors and broader interop assertion fixtures are added by later
feature tasks.
