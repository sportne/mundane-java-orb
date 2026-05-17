# Reference Implementations and Interoperability Peers

## Peers

| Peer | Role |
|---|---|
| JacORB | Java ORB interoperability and behavioral reference. |
| Eclipse GlassFish CORBA ORB | Jakarta/GlassFish lineage interoperability and behavioral reference. Source repository: `eclipse-ee4j/orb`. |
| JBoss OpenJDK ORB | Legacy Java/OpenJDK ORB and JBoss application-server behavior reference. |
| ACE/TAO | C++ ORB and cross-language interoperability reference. |

## Allowed use

- Black-box interoperability tests.
- Behavioral observations through logs, IORs, wire captures, and test output.
- Documentation review.
- Clean-room notes with source, version, license, and observed behavior.

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
