# Documentation Policy

## Repository documentation

Every major project area must have a design document under `docs/` before
implementation begins.

## Code documentation

- Every public API package must have `package-info.java`.
- Every published module must have module documentation once JPMS metadata is
  introduced.
- Every public class, interface, enum, annotation, and public method must have
  Javadoc.
- Protocol parser/writer classes must document bounds, invariants, and
  specification references.
- Generated code must include generator version, source IDL identity, mapping
  mode, and compatibility profile.

## Agent rule

No implementation PR is acceptable unless it updates at least one relevant
requirement, architecture, verification, conformance, module README, package-info,
or Javadoc artifact.
