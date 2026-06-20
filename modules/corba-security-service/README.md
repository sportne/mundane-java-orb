# corba-security-service

CORBA Security / CSIv2 implementation module.

## Current status

Design accepted by ADR-0022. `G8-610-SECURITY-SERVICE-CREDENTIAL-TRUST-MODEL`
adds bounded local credential and trust primitives: principal IDs, credential
IDs/kinds, credential lifetime metadata, trust anchors, immutable snapshots,
trust-evaluation inputs, and stable malformed, duplicate, missing, expired,
untrusted, and limit diagnostics. `G8-620-SECURITY-SERVICE-POLICY-MODEL` adds
bounded policy objects and validation for authentication, trust, transport
protection, identity assertion, delegation, and audit settings with stable
malformed, duplicate, conflicting, unsupported-delegation, and limit
diagnostics. `G8-630-SECURITY-SERVICE-CSIV2-METADATA-MODEL` adds bounded CSIv2
metadata primitives and deterministic project-owned encode/decode for the
supported local mechanism identity, transport-protection flags, identity-token
policy, and target/client authentication metadata.
`G8-640-SECURITY-SERVICE-LOCAL-POLICY-EVALUATION` adds deterministic local
allow, challenge, and deny decisions over the credential/trust, policy, and
CSIv2 metadata models with stable missing credential, expired credential,
untrusted credential, unsupported delegation, transport protection, malformed
metadata, and policy-conflict diagnostics.
`G8-650-SECURITY-SERVICE-AUDIT-FAILURE-DISCLOSURE` adds bounded redacted audit
events and stable failure disclosures for local decisions and Security Service
exceptions without copying raw exception messages or credential material.
`G8-660-SECURITY-SERVICE-IIOP-BOUNDARY` adds a descriptor-backed loopback IIOP
boundary for bounded CSIv2 service-context and tagged-component handling, local
policy allow/challenge/deny evaluation, redacted audit events, malformed
context diagnostics, and clean shutdown.
`G8-670-SECURITY-SERVICE-NATIVE-SMOKE` adds deterministic Native Image smoke
coverage for credential rejection, trust validation, policy rejection, CSIv2
metadata encode/decode, local policy evaluation, audit redaction, IIOP boundary
behavior, clean shutdown, and source-level closed-world audits over Security
Service production sources. `G8-680-SECURITY-SERVICE-INTEROP-METADATA`
adds the `security-service` IDL fixture, approved peer manifest declarations,
`InteropScenario.securityService()`, deterministic JVM/native dry-run direction
enumeration, and structured missing-prerequisite reports without starting peer
containers or local live lanes. `G8-690-SECURITY-SERVICE-CONFORMANCE-CLOSURE`
closes the implemented Security Service / CSIv2 conformance record for the
credential/trust model, policy validation, CSIv2 metadata, local policy
evaluation, audit/failure disclosure, descriptor-backed loopback IIOP boundary,
Native Image smoke, and metadata-only interop dry-run subset. Live secure peer
execution, pass/fail peer compatibility claims, enterprise identity management,
automatic TLS policy changes, and global JVM security-manager integration remain
unapproved. No next ready Security Service task is promoted by the closure.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
