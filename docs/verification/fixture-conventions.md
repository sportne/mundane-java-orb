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

This convention defines reusable test infrastructure only. Real IDL compiler
golden output, protocol byte vectors, and interop assertion fixtures are added by
later feature tasks.
