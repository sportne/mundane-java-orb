# Conformance Index

Conformance matrix files:

- `corba-3.4-matrix.md`
- `corba-3.3-matrix.md`
- `corba-3.2-matrix.md`
- `corba-2.3-legacy-java-matrix.md`
- `idl-4.2-matrix.md`
- `idl-to-java-matrix.md`
- `naming-service-matrix.md`

## Status values

| Status | Meaning |
|---|---|
| not-started | No design or implementation. |
| designed | Architecture/design approved. |
| partial | Some behavior is implemented and tested, with remaining gaps explicitly listed in the row notes. |
| implemented | Code implemented. |
| unit-tested | Unit/spec tests exist. |
| golden-tested | Golden-source or golden-wire tests exist. |
| integration-tested | Local integration tests exist. |
| interop-tested | External ORB tests exist. |
| native-tested | Native Image tests exist. |
| deferred | Explicitly deferred by ADR. |

Rows with `partial` status must name the implemented subset, test evidence, and
remaining unsupported behavior in the Notes column. G6-930 keeps these rows as
`partial` instead of overstating conformance for slices that have local
implementation and verification but not full spec, external interop, or
native-image coverage.
