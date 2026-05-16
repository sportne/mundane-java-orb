# Security Policy

This project will eventually process untrusted network input. Security controls
are therefore part of the architecture, not a late-stage hardening task.

## Initial rules

- All protocol inputs must be bounded before allocation.
- CDR, GIOP, IIOP, IOR, `corbaloc`, and `corbaname` parsers require negative and
  fuzz tests.
- Java serialization is forbidden for normal CORBA marshaling.
- Reflection, runtime bytecode generation, and dynamic proxies are forbidden in
  core modules unless a dedicated ADR grants a narrow exception.
- TLS and mTLS behavior must be explicitly configured; global JVM state is not an
  acceptable security mechanism.

## Reporting

This scaffold has no public vulnerability intake process yet. Before external
release, add a security contact, disclosure process, and supported-version matrix.
