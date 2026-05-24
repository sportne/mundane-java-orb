# Module Boundaries

## Hard rules

The repository-wide rule source is `architecture-rule-catalog.md`. This page
summarizes the module-layering subset of that catalog.

- Only `corba-omg-api` may define `org.omg.*` packages.
- `corba-omg-api` owns legacy source-compatibility signatures only; runtime
  ORB, POA, Naming, DynamicAny, interceptor, protocol, and service behavior
  remains in the implementation modules assigned by roadmap task.
- Protocol modules must not depend on ORB core.
- IDL modules must not depend on transport modules.
- `corba-cdr` must not depend on GIOP, IIOP, ORB, POA, or services.
- `corba-giop` may depend on CDR and IOR, but not ORB core.
- `corba-iiop` may depend on GIOP, CDR, IOR, and common modules.
- `corba-poa` may depend on ORB core; protocol modules must not depend on POA.
- Reflection is forbidden in core modules unless an ADR grants a waiver.
- Runtime bytecode generation is forbidden in normal runtime paths.

## Enforcement

The rules are enforced by:

- ArchUnit tests in `modules/corba-architecture-tests`, including staged
  checks for OMG package ownership, IDL isolation from transport/protocol
  packages, protocol-to-runtime separation, CDR/GIOP/IIOP dependency limits,
  Native Image restrictions, Java serialization restrictions, baseline Java
  safety restrictions, and runtime bytecode-generation restrictions;
- static analysis;
- coding-agent rules;
- module dependency reviews.

During the G6 foundation phase, architecture tests still allow empty package
matches for future modules so rules can exist before the corresponding packages
are implemented. Implemented foundation packages are no longer purely scaffolded:
`io.github.mundanej.mjo.common` must be visible to architecture checks and must
not depend on feature modules.
