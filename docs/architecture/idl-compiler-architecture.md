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

The semantic model covers the parser-approved subset only. G10-010 extends the
front end for the non-optional pre-1.0 interop IDL corpus:

- module, interface, operation, attribute, struct, field, enum, enumerator,
  exception, constant, parameter, typedef, and union symbols;
- absolute qualified names using IDL `::` spelling;
- builtin primitive type references, bounded `string`/`wstring`,
  `sequence<T>` and `sequence<T, bound>`, fixed-array declarators, and
  user-defined struct, union, enum, exception, interface, and typedef type
  references;
- case-insensitive duplicate detection within each semantic scope;
- simple declaration-order constant evaluation;
- `raises(...)` validation against previously declared exceptions;
- interface forward declarations, interface inheritance, and inheritance-cycle
  diagnostics;
- union discriminator, case/default label, duplicate-label, and member-type
  validation.

The analyzer uses two-pass collection for type names so fields, attributes,
operation returns, parameters, typedefs, unions, and recursive type references
may refer to types declared later in the same translation unit. Constant
references, array/sequence/string bounds, and `raises(...)` targets remain
declaration-order constrained in this slice.

The semantic model is deliberately not a compiler back end. Repository ID
construction, Java mapping, generated source, generated codecs, TypeCode, Any,
CDR, GIOP/IIOP, ORB runtime behavior, reflection, dynamic classpath scanning,
and Native Image metadata generation remain assigned to later roadmap tasks.

## Validation CLI Boundary

The first `corba-idlj` command-line slice exposes validation only:

```text
corba-idlj validate [-Ipath|-I path|--include path] [--quiet] <files...>
```

The command reads UTF-8 root IDL files in argument order, expands includes
through explicit include roots, parses the approved minimal subset, and runs
semantic analysis only when parsing succeeds. It emits deterministic human
diagnostics to stderr and returns stable exit codes for success, validation
failure, and command/input failure.

This boundary intentionally stops before compiler back-end behavior. The CLI
does not generate Java source, CDR codecs, repository IDs, descriptors, ORB
runtime artifacts, protocol behavior, or Native Image metadata in G6-150.

## Java Mapping And Source Boundary

The first source-generation slice consumes a valid `IdlSemanticModel` and emits
an intermediate `JavaMappingModel` before rendering Java source. Mapping is
explicitly mode-specific:

- `LEGACY_COMPATIBILITY` uses conservative legacy-oriented naming while staying
  compile-safe without legacy runtime artifacts;
- `MODERN` maps the same semantic subset into a distinct modern package
  namespace.

G6-160 covers modules, interfaces, operations, attributes, structs, enums,
exceptions, and constants for the parser-approved minimal subset. Generated
source is deterministic, includes source IDL identity and mapping metadata, and
compiles without `org.omg.*`, ORB runtime APIs, CDR codecs, repository IDs,
helpers, holders, stubs, skeletons, POA classes, reflection, dynamic class
loading, or Native Image metadata.

G7-050 adds compatibility tests proving the approved RMI generated-IDL fixture
continues to map through this IDL-to-Java model and compile through the existing
source renderer. The RMI-specific helper, holder, stub, tie, skeleton
placeholder, and binding-descriptor surfaces are generated in
`corba-rmi-iiop`, not by the generic IDL compiler pipeline.

## Descriptor And Codec Boundary

G6-220 adds a source-generation-only descriptor pass after Java mapping. It
emits generated Java source for static IDL type and operation descriptors plus
compile-only codec surfaces. Generated descriptor source may reference
`corba-typecode`, `corba-cdr`, and `corba-repository-id`, but the codegen module
itself remains a source renderer and does not depend on protocol/runtime
packages.

The G6-220 codec classes are intentionally nonfunctional. They expose stable
`IdlCodec<T>` fields and fail with `UnsupportedOperationException` until G6-320
adds bounded CDR string, sequence, array, and encapsulation handling. The pass
does not add CLI generation commands, ORB invocation behavior, GIOP/IIOP
transport, runtime registries, reflection metadata, helpers, holders, stubs,
skeletons, or POA artifacts.
