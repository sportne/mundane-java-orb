# Standards Baseline

## Primary baseline

The primary normative implementation target is CORBA 3.4.

## Formal compatibility baselines

- CORBA 3.3.
- CORBA 3.2.

## Practical legacy compatibility baseline

- Java/CORBA 2.3.x-era behavior where necessary for interoperability with
  legacy Java ORBs and application-server deployments.

## IDL baseline

- OMG IDL 4.2.

## Java mapping baselines

- OMG IDL to Java Language Mapping for legacy mode.
- OMG IDL4 to Java Language Mapping as design input for modern mode.
- OMG Java to IDL Language Mapping for RMI-IIOP work, staged separately.

## Specification index

| Area | Primary reference |
|---|---|
| CORBA 3.4 | https://www.omg.org/spec/CORBA/3.4/About-CORBA |
| CORBA 3.4 Interfaces | https://www.omg.org/spec/CORBA/3.4/Interfaces/PDF |
| CORBA 3.4 Interoperability | https://www.omg.org/spec/CORBA/3.4/Interoperability/PDF |
| CORBA 3.4 Components | https://www.omg.org/spec/CORBA/3.4/Components/PDF |
| OMG IDL 4.2 | https://www.omg.org/spec/IDL/4.2/About-IDL |
| IDL to Java Mapping | https://www.omg.org/spec/I2JAV/1.3/About-I2JAV |
| Java to IDL Mapping | https://www.omg.org/spec/JAV2I/1.4/ |
| Naming Service | https://www.omg.org/spec/NAM/1.3/About-NAM |

## Documentation rule

Every requirement and every protocol design document must reference a
specification and, once detail design begins, the relevant clause or section.

## Local specification cache

Agents and maintainers may keep local working copies of official specification
PDFs under `reference/specs/`. The PDF files in that directory are intentionally
ignored by Git; only `reference/specs/README.md` is tracked to document source
URLs and naming conventions.

The local cache is a reading aid only. Requirements, conformance matrices, ADRs,
and implementation tasks must cite the canonical references in
`docs/specification-traceability.md`.
