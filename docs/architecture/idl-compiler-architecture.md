# IDL Compiler Architecture

## Pipeline

```text
IDL source files
  -> include resolver
  -> preprocessor
  -> lexer
  -> parser
  -> AST
  -> semantic model
  -> repository ID resolver
  -> Java mapping
  -> generated Java source
  -> generated CDR codecs
  -> generated descriptors
  -> generated native-image metadata
```

## Design rules

- Diagnostics must have stable codes.
- Output must be deterministic.
- Compatibility and modern generation modes must be separate.
- Generated code must not require reflection in normal invocation paths.
- Generated source snapshots must be tested.

## Lexer And Preprocessor Boundary

The lexer owns OMG IDL lexical tokenization and emits stable `IDL-01xx`
diagnostics. It preserves source lexemes and source spans, but it does not
evaluate literal values or interpret directive semantics.

The preprocessor owns the first translation-unit token stream. It normalizes
backslash-newline continuations before lexing, then remaps emitted token spans
and diagnostics to original source positions. Directive handling is limited to
the approved first slice:

- `#include "name.idl"` and `#include <name.idl>` expansion through explicit
  include resolvers;
- nested include expansion with cycle diagnostics and include-depth limits;
- object-like macros, simple function-like macros, `#undef`, and bounded macro
  expansion;
- `#ifdef`, `#ifndef`, selected `#if`/`#elif` expressions, `#else`, and
  `#endif`;
- `#pragma` token pass-through without semantic interpretation.

Include resolution is read-only and root-bounded. The path resolver rejects
absolute include names, parent traversal, backslash separators, and normalized
paths that escape configured roots. Included-source tokens keep spans for the
included source. Macro replacement tokens use the invocation span, while macro
argument tokens preserve the argument token spans.

Full ISO C++ 2003 preprocessor compatibility is deferred. Variadic macros,
token pasting, stringification, arithmetic preprocessor expressions, predefined
macro sets, and full macro-rescan corner cases must produce explicit
diagnostics or remain assigned to future conformance-hardening work until they
are implemented.

## Semantic Model Boundary

The semantic analyzer owns the first syntax-to-symbol pass after parsing. It
accepts an `IdlTranslationUnit`, never reparses source text, and emits either a
deterministic `IdlSemanticModel` or stable `IDL-04xx` diagnostics.

The G6-140 model covers the parser-approved subset only:

- module, interface, operation, attribute, struct, field, enum, enumerator,
  exception, constant, and parameter symbols;
- absolute qualified names using IDL `::` spelling;
- builtin primitive type references and user-defined struct, enum, exception,
  and interface type references;
- case-insensitive duplicate detection within each semantic scope;
- simple declaration-order constant evaluation;
- `raises(...)` validation against previously declared exceptions.

The analyzer uses two-pass collection for type names so fields, attributes,
operation returns, and parameters may refer to types declared later in the same
translation unit. Constant references and `raises(...)` targets remain
declaration-order constrained in this slice.

The semantic model is deliberately not a compiler back end. Repository ID
construction, Java mapping, generated source, generated codecs, TypeCode, Any,
CDR, GIOP/IIOP, ORB runtime behavior, reflection, dynamic classpath scanning,
and Native Image metadata generation remain assigned to later roadmap tasks.
