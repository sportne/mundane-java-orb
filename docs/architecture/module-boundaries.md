# Module Boundaries

## Hard rules

- Only `corba-omg-api` may define `org.omg.*` packages.
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

- ArchUnit tests in `modules/corba-architecture-tests`;
- static analysis;
- coding-agent rules;
- module dependency reviews.
