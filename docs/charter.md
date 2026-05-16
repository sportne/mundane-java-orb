# Project Charter

## Mission

Build a complete, modular, specification-traced Java CORBA ecosystem that can
support legacy CORBA compatibility, modern generated-code APIs, GraalVM Native
Image execution, and interoperability with selected Java and C++ ORB
implementations.

## Strategic goals

1. Replace the removed JDK CORBA ecosystem with independently usable artifacts.
2. Preserve practical interoperability with deployed CORBA systems.
3. Provide a modern implementation architecture using generated code, bounded
   protocol parsers, explicit configuration, and strong operational diagnostics.
4. Make Native Image support a first-class design constraint.
5. Make the repository safe for coding-agent-driven implementation through
   requirement traceability, architecture rules, documentation requirements, and
   build gates.

## Current gate

This scaffold supports G0/G1/G4 preparation. Runtime implementation must not begin
until G0 through G5 are approved.

## Non-goals for this scaffold

- No CDR implementation.
- No GIOP/IIOP implementation.
- No IDL parser.
- No ORB or POA implementation.
- No copied source from reference ORBs.
