# RMI-IIOP and Java-to-IDL Design

RMI-IIOP is a legacy compatibility feature for exposing eligible Java remote
interfaces through IDL and IIOP. It belongs to the `LEGACY_JAVA_CORBA`
compatibility profile and is governed by accepted ADR-0013.

## Scope

The design gate covers:

- Java-to-IDL mapping for the RMI/IDL subset defined by JAV2I-14;
- repository ID handling for Java RMI forms already recognized by the
  repository-id foundation;
- generated IDL and Java binding surfaces needed by future RMI-IIOP adapters;
- local and wire invocation plans that reuse the existing ORB, POA, CDR, GIOP,
  IIOP, and interop infrastructure.

The design gate does not cover:

- public runtime APIs;
- generated source output;
- functional stub, tie, skeleton, or helper implementation;
- peer interop execution;
- automatic classpath scanning or reflective adaptation of arbitrary classes.

## Module Boundary

`modules/corba-rmi-iiop` is the staging module for future RMI-IIOP adapters and
public compatibility APIs. It must not become a second ORB, transport, CDR
engine, or IDL compiler.

Implementation tasks should keep responsibilities split this way:

- Java type eligibility and Java-to-IDL planning belong in `corba-rmi-iiop`.
- Repository ID parsing and validation stay in `corba-repository-id`.
- IDL syntax and semantic models stay in the existing IDL modules.
- Source emission stays in `corba-codegen` and mapping modules.
- Invocation dispatch stays in `corba-orb-core` and `corba-poa`.
- Wire encoding and transport stay in `corba-cdr`, `corba-giop`, and
  `corba-iiop`.
- Peer execution and reports stay in `corba-interop-testkit` and `interop/`.

## Data Flow

Future implementation should use an explicit, generated-data flow:

```text
Java remote interface declaration
  -> eligibility and signature model (G7-010)
  -> Java-to-IDL model (G7-020)
  -> explicit RMI repository ID planning (G7-030)
  -> generated IDL fixture and IDL validation (G7-040)
  -> generated binding source surfaces and local descriptors (G7-050/G7-070)
  -> bounded local value and empty user-exception CDR codecs (G7-060)
  -> local ORB/POA invocation adapters (G7-070)
  -> bounded local JVM CDR/GIOP/IIOP transport (G7-080)
```

No step should depend on runtime classpath scanning. If a later implementation
needs information from Java source or bytecode, that task must define an
explicit input file, parser, generated metadata format, or build-time tool.

## Compatibility Rules

RMI-IIOP behavior is controlled by the `LEGACY_JAVA_CORBA` profile. Future tasks
must name the exact JAV2I-14 section and the affected compatibility profile row
before adding behavior.

The implementation slices should prefer observable, low-risk behavior:

- classify supported and unsupported Java remote interface shapes (started by
  G7-010);
- produce deterministic diagnostics for unsupported Java-to-IDL inputs (started
  by G7-020);
- plan RMI repository IDs from explicit hash and serialVersionUID metadata
  without Java serialization or class loading (started by G7-030);
- generate small IDL golden fixtures from approved Java inputs (started by
  G7-040);
- generate compile-safe Java binding surfaces and local adapter descriptors
  (started by G7-050 and extended by G7-070);
- marshal approved primitive/String values and empty declared user exceptions
  through bounded local CDR payloads without Java serialization (started by
  G7-060);
- invoke approved generated stubs, ties, and skeletons through in-process
  `LocalOrb` and `Poa` paths (started by G7-070);
- invoke approved generated wire stubs through existing `IiopClient` /
  `IiopServer` request/reply paths for bounded local JVM scenarios without peer
  compatibility claims (started by G7-080);
- preserve and validate RMI repository ID forms;
- prove local adapter and local wire invocation before external peer claims.

## Native Image and Security Rules

Normal runtime paths must avoid:

- reflection-driven invocation or marshaling;
- dynamic proxies;
- runtime bytecode generation;
- Java serialization as the marshaling mechanism;
- unbounded classpath scanning;
- `Unsafe`, `sun.*`, and `jdk.internal.*`.

Future tasks must define bounds and diagnostics for hostile inputs, including
Java names, repository IDs, generated IDL, value payloads, exception payloads,
and remote object references.

## Verification Plan

Future implementation tasks must add the narrowest useful coverage for each
slice:

- unit tests for eligibility, mapping, and diagnostics;
- golden IDL or golden Java fixtures for generated output;
- local integration tests for ORB/POA adapter behavior;
- local JVM wire integration tests for bounded GIOP/IIOP request/reply behavior;
- interop scenarios against approved Java ORB peers before compatibility claims;
- Native Image smoke coverage for public adapter entrypoints.

After G7-080, this module contains explicit Java declaration models,
deterministic eligibility diagnostics, an in-memory Java-to-IDL mapping model,
metadata-based RMI repository ID planning, generated IDL fixtures for the
parser-supported subset, compile-safe Java binding surfaces, bounded local CDR
codecs for approved primitive/String values and empty declared user exceptions,
generated local ORB/POA adapter invocation, and bounded local JVM GIOP/IIOP wire
integration for the approved binding slice. Follow-on G7 tasks must still
verify peer interop and Native Image closure before external compatibility
claims are made.
