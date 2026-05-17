# corba-idl-parser

IDL lexer, preprocessor, parser, include resolver, and diagnostics.

## Current status

G6 lexer implementation has started with bounded OMG IDL 4.2 lexical scanning,
stable token values, source spans, and diagnostics.

This module still does not implement parser grammar, include resolution,
preprocessor macro semantics, AST construction, semantic analysis, code
generation, ORB/runtime behavior, or protocol behavior.

## Lexer behavior

- Tokenizes identifiers, escaped identifiers, exact-case keywords, literals,
  comments, whitespace, punctuation, and preprocessor tokens from OMG IDL 4.2
  lexical conventions.
- Preserves source lexemes and source spans; literal values are not evaluated or
  unescaped in this slice.
- Emits stable `IDL-01xx` diagnostics for malformed lexical input and configured
  scanning limit violations.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
