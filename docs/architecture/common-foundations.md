# Common Foundations

`corba-common` contains small immutable values shared by parser, protocol,
runtime, and tooling modules. It must not contain parser logic, CDR/GIOP/IIOP
encoding, ORB invocation, service behavior, generated code, reflection-based
dispatch, or Java serialization for normal CORBA behavior.

## Diagnostics

Diagnostics use stable `AREA-0000` codes, a severity, a nonblank message, and an
optional source span. Source positions use one-based line and column numbers and
zero-based offsets so parser and tooling modules can report deterministic
locations without depending on parser internals.

## Bounded limits

Network- or source-originating sizes must be checked before allocation in the
module that consumes them. `BoundedLimit` is a shared value for naming those
limits and reporting deterministic violations; it is not a global configuration
system.

## Ownership rule

Feature modules own their behavior. `corba-common` may provide reusable values,
but feature-specific parsing, marshaling, invocation, and policy decisions
belong in their dedicated modules.
