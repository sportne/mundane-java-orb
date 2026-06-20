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
context diagnostics, and clean shutdown. `G8-670` is the only ready successor
and is reserved for Native Image smoke coverage. Interop metadata and
conformance closure remain blocked behind their predecessors.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
