# ADR-0022: Security Service And CSIv2 Design

Status: accepted
Date: 2026-05-31
Decision owner: Maintainers

Approved: 2026-05-31 by G8 optional services look-back approval in the project thread.

## Context

Security Service / CSIv2 affects credentials, trust, authorization, auditing,
IIOP security components, and failure disclosure. It is the highest-risk
optional service and should follow the other service design groups.

## Decision

Approve a staged Security Service / CSIv2 design in
`modules/corba-security-service`.

The supported design subset is:

- explicit credential and trust model before any secure invocation behavior;
- policy model with deterministic validation and no ambient global JVM state;
- CSIv2 metadata parsing/formatting staged before enforcement;
- local policy evaluation before IIOP integration;
- audit event model that avoids leaking secrets in diagnostics;
- Native Image smoke coverage for credential rejection, policy validation, and
  bounded CSIv2 metadata handling.

Non-goals are full enterprise identity management, dynamic credential discovery,
global security-manager integration, automatic TLS policy changes, and live peer
claims before dedicated interop tasks.

## Consequences

Security Service is the final recommended optional-service group. It must not
be used to weaken existing endpoint-local TLS/mTLS behavior or introduce
reflection-heavy security frameworks.

## Alternatives considered

- Implement Security Service before other optional services: rejected because
  its policy and interop risk are highest.
- Bind directly to JVM-global security facilities: rejected because the project
  requires explicit configuration and Native Image-friendly behavior.

## Specification references

- SEC-18
- CORBA-IOP-SECURITY

## Requirements affected

- REQ-SVC-050
- REQ-IIOP-002
- REQ-NATIVE-002
- REQ-INTEROP-009
- REQ-SEC-006
- REQ-DOC-006

## Build/test impact

Follow-on tasks must add policy tests, credential/trust tests, CSIv2 metadata
tests, Native Image smoke, and structured interop metadata.

## Native-image impact

Credentials, policies, and trust inputs must be explicit runtime objects. No
classpath scanning, dynamic proxies, Java serialization metadata, or reflective
security framework discovery is allowed.

## Interop impact

No live peer claim is accepted yet. Future scenarios should cover CSIv2 metadata
and secure invocation only after local policy behavior is stable.

## Security impact

Credential lifetime, trust roots, policy validation, audit content, downgrade
behavior, and failure disclosure require dedicated review before implementation.
