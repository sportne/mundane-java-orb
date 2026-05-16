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
