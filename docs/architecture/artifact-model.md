# Artifact Model

The project publishes independent artifacts so downstream users can consume only
what they need.

## Artifact families

- API artifacts: `corba-omg-api`, `corba-modern-api`.
- Compiler artifacts: `corba-idl-*`, `corba-codegen`, `corba-idlj-cli`.
- Protocol artifacts: `corba-cdr`, `corba-ior`, `corba-giop`, `corba-iiop`.
- Runtime artifacts: `corba-orb-core`, `corba-poa`, `corba-interceptors`.
- Dynamic artifacts: `corba-any`, `corba-typecode`, `corba-dynamic`,
  `corba-interface-repository`.
- Service artifacts: naming and optional CORBA services.
- Native-image artifact: `corba-native-image`.
- Test artifacts: `corba-testkit`, `corba-interop-testkit`.
- Alignment artifact: `corba-bom`.

## Rule

Every artifact must have a clear standalone reason to exist and a documented
public API or explicit internal/test-only status.
