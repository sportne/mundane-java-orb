# corba-repository-id

Repository ID parsing, creation, validation, and normalization.

## Current status

G6 repository ID foundation behavior is implemented here. The module provides
string/value rules for CORBA RepositoryIds:

- parses any syntactically valid `<format>:<string>` form;
- validates and normalizes `IDL:<path>:<major>.<minor>` values;
- validates, normalizes, and constructs `RMI:<javaBinaryName>:<hash>[:<uid>]`
  values from explicit metadata inputs;
- preserves recognized `DCE:` and `LOCAL:` forms deterministically;
- preserves unknown formats for future registered or implementation-specific
  repository ID schemes;
- constructs common IDL-format values from paths and scoped-name segments.

This module does not parse IOR binary profiles, integrate with TypeCode, process
IDL pragmas, compute Java RMI hash or serialVersionUID values, generate source
code, or start ORB runtime behavior.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
