# Reference Implementations and Interoperability Peers

## Peers

| Peer | Role | G6-820 gate |
|---|---|---|
| JacORB | Java ORB interoperability and behavioral reference. | Approved for black-box interop through `interop/approvals/jacorb.approval.yaml`. |
| Eclipse GlassFish CORBA ORB | Jakarta/GlassFish lineage interoperability and behavioral reference. Source repository: `eclipse-ee4j/orb`. | Approved for black-box interop through `interop/approvals/glassfish-orb.approval.yaml`. |
| JBoss OpenJDK ORB | Legacy Java/OpenJDK ORB and JBoss application-server behavior reference. | Approved for black-box interop through `interop/approvals/jboss-openjdk-orb.approval.yaml`. |
| ACE/TAO | C++ ORB and cross-language interoperability reference. | Approved for black-box interop through `interop/approvals/ace-tao.approval.yaml`. |

## Allowed use

- Black-box interoperability tests.
- Behavioral observations through logs, IORs, wire captures, and test output.
- Documentation review.
- Clean-room notes with source, version, license, and observed behavior.

Approved artifacts must live outside the repository in `INTEROP_ARTIFACT_CACHE`
and match the SHA-256 values recorded in `interop/approvals/`.

## Prohibited use

- Copying source code.
- Transliteration of implementation logic.
- Agent-driven imitation of source files.
- Using GPL/license-sensitive source as implementation material without legal review.

## Required clean-room note fields

- peer name;
- peer version;
- source or binary origin;
- license observed;
- scenario executed;
- observed behavior;
- generated test case, if any;
- reviewer.
