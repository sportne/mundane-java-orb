# corba-idl-parser

IDL lexer, preprocessor, parser, include resolver, and diagnostics.

## Current status

G6 IDL compiler foundation work has started with bounded OMG IDL 4.2 lexical
scanning and a minimal C-style preprocessing slice.

This module still does not implement parser grammar, AST construction, semantic
analysis, code generation, ORB/runtime behavior, or protocol behavior.

## Lexer behavior

- Tokenizes identifiers, escaped identifiers, exact-case keywords, literals,
  comments, whitespace, punctuation, and preprocessor tokens from OMG IDL 4.2
  lexical conventions.
- Preserves source lexemes and source spans; literal values are not evaluated or
  unescaped in this slice.
- Emits stable `IDL-01xx` diagnostics for malformed lexical input and configured
  scanning limit violations.

## Preprocessor behavior

- Normalizes backslash-newline continuations before token processing while
  mapping emitted token spans and diagnostics back to the original source.
- Expands quoted and system includes through caller-supplied read-only include
  resolvers, including nested includes, cycle diagnostics, unsafe-path
  rejection, and include-depth limits.
- Supports object-like macros, simple function-like macros, `#undef`,
  `#ifdef`, `#ifndef`, a bounded subset of `#if`/`#elif`, `#else`, `#endif`,
  and `#pragma` token pass-through.
- Emits stable `IDL-02xx` diagnostics for unsupported first-slice
  preprocessing constructs. Variadic macros, token pasting, stringification,
  arithmetic preprocessor expressions, predefined macros, and full C++ 2003
  compatibility remain deferred.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
