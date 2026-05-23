# Specification Traceability

Every requirement must trace to one or more specification references or to an
explicit compatibility/operational requirement.

## Canonical specification references

Use these short reference keys in requirement tables, conformance matrices, and
implementation tasks. Clause references are intentionally section-level during
G1; implementation tasks must narrow them further when a feature is designed.

| Key | Specification reference |
|---|---|
| CORBA-IF | CORBA 3.4 Part 1: Interfaces, formal/21-02-02 |
| CORBA-IF-OBJECT | CORBA-IF section 5, The Object Model |
| CORBA-IF-OVERVIEW | CORBA-IF section 6, CORBA Overview |
| CORBA-IF-ORB | CORBA-IF section 8, ORB Interface |
| CORBA-IF-OBJECT-REF | CORBA-IF section 8.3, Object Reference Operations |
| CORBA-IF-TYPECODE | CORBA-IF section 8.11, TypeCodes |
| CORBA-IF-VALUES | CORBA-IF section 9, Value Type Semantics |
| CORBA-IF-DII | CORBA-IF section 11, Dynamic Invocation Interface |
| CORBA-IF-DSI | CORBA-IF section 12, Dynamic Skeleton Interface |
| CORBA-IF-DYNANY | CORBA-IF section 13, Dynamic Management of Any Values |
| CORBA-IF-IR | CORBA-IF section 14, The Interface Repository |
| CORBA-IF-POA | CORBA-IF section 15, The Portable Object Adapter |
| CORBA-IF-PI | CORBA-IF section 16, Portable Interceptors |
| CORBA-IF-MESSAGING | CORBA-IF section 17, CORBA Messaging |
| CORBA-IOP | CORBA 3.4 Part 2: Interoperability, formal/12-11-14 |
| CORBA-IOP-ARCH | CORBA-IOP sections 6 and 7, Interoperability Overview and ORB Interoperability Architecture |
| CORBA-IOP-IOR | CORBA-IOP section 7.6, An Information Model for Object References |
| CORBA-IOP-OBJECT-URL | CORBA-IOP sections 7.6.9 and 7.6.10, Stringified Object References and Object URLs |
| CORBA-IOP-SERVICE-CONTEXT | CORBA-IOP section 7.7, Service Context |
| CORBA-IOP-CODE-SET | CORBA-IOP section 7.10, Code Set Conversion |
| CORBA-IOP-CDR | CORBA-IOP section 9.3, CDR Transfer Syntax |
| CORBA-IOP-GIOP | CORBA-IOP sections 9.2 through 9.6, GIOP Overview, Message Formats, Message Transport, and Object Location |
| CORBA-IOP-IIOP | CORBA-IOP section 9.7, Internet Inter-ORB Protocol |
| CORBA-IOP-SECURITY | CORBA-IOP section 10, Secure Interoperability |
| IDL-42 | OMG IDL 4.2, formal/18-01-05 |
| IDL-42-LEXICAL | IDL-42 section 7.2, Lexical Conventions |
| IDL-42-PREPROCESSING | IDL-42 section 7.3, Preprocessing |
| IDL-42-GRAMMAR | IDL-42 section 7.4 and Annex A, IDL Grammar |
| IDL-42-SCOPING | IDL-42 section 7.5, Names and Scoping |
| IDL-42-ANNOTATIONS | IDL-42 section 8, Standardized Annotations |
| IDL-42-PROFILES | IDL-42 section 9, Profiles |
| I2JAV-13 | IDL to Java Language Mapping 1.3, formal/08-01-11 |
| I2JAV-13-MODULES | I2JAV-13 section 4.3, Mapping of Module |
| I2JAV-13-BASIC | I2JAV-13 section 4.4, Mapping for Basic Types |
| I2JAV-13-HELPERS | I2JAV-13 section 4.5, Helpers |
| I2JAV-13-CONSTANTS | I2JAV-13 section 4.6, Mapping for Constant |
| I2JAV-13-TYPES | I2JAV-13 sections 4.7 through 4.11 and 4.18, Enum, Struct, Union, Sequence, Array, and Typedef mappings |
| I2JAV-13-INTERFACES | I2JAV-13 section 4.12, Mapping for Interface |
| I2JAV-13-VALUES | I2JAV-13 sections 4.13 and 4.14, Value Type and Value Box mappings |
| I2JAV-13-EXCEPTIONS | I2JAV-13 section 4.15, Mapping for Exception |
| I2JAV-13-ANY | I2JAV-13 section 4.16, Mapping for the Any Type |
| I2JAV-13-PSEUDO | I2JAV-13 section 4.19, Mapping Pseudo Objects to Java |
| I2JAV-13-SERVER | I2JAV-13 section 4.20, Server-Side Mapping |
| I2JAV-13-PORTABILITY | I2JAV-13 section 4.21, Java ORB Portability Interfaces |
| JAV2I-14 | Java to IDL Language Mapping 1.4, formal/08-01-14 |
| JAV2I-14-RMI-IDL | JAV2I-14 sections 1.2 and 1.3, RMI/IDL subset and Java-to-IDL mapping |
| NAM-13 | Naming Service 1.3, formal/04-10-03 |
| NAM-13-SERVICE | NAM-13 section 1, Service Description |
| NAM-13-COSNAMING | NAM-13 section 2.1, The CosNaming Module |
| NAM-13-CONTEXT | NAM-13 section 2.2, NamingContext Interface |
| NAM-13-ITERATOR | NAM-13 section 2.3, BindingIterator Interface |
| NAM-13-STRINGIFIED | NAM-13 section 2.4, Stringified Names |
| NAM-13-URLS | NAM-13 section 2.5, URL schemes |
| NAM-13-LIGHTWEIGHT | NAM-13 section 3, Lightweight Naming Service |
| TRADE-10 | Trading Object Service 1.0, formal/00-06-27 |
| EVNT-12 | Event Service 1.2, formal/04-10-02 |
| NOT-11 | Notification Service 1.1, formal/04-10-13 |
| TRANS-14 | Transaction Service 1.4, formal/03-09-02 |
| SEC-18 | Security Service 1.8, formal/02-03-11 |
| TIME-11 | Time Service 1.1, formal/02-05-06 |

## Required fields

```yaml
requirementId: REQ-CDR-001
title: Decode primitive CDR values
specReferences:
  - spec: CORBA-IOP
    section: "9.3.1 Primitive Types"
    note: CDR primitive encoding and alignment
compatibilityProfiles:
  - CORBA_3_4_FULL
  - CORBA_3_3_COMPAT
  - CORBA_3_2_COMPAT
verification:
  - unit
  - golden-wire
  - fuzz
  - native-image
```

## Traceability levels

| Level | Meaning |
|---|---|
| Requirement | What must be true. |
| Design | How the requirement is satisfied architecturally. |
| Test | How the behavior is verified. |
| Conformance matrix | Whether the spec feature is implemented, tested, interop-tested, and native-tested. |
| Implementation task | The narrow agent-executable work item. |

## Rule

A feature is not complete until all traceability levels are updated.
