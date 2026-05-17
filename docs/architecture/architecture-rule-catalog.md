# Architecture Rule Catalog

This catalog records architecture rules that are mechanically enforced where
practical. Rules apply to production main code unless an entry says otherwise.
Exceptions require a narrow ADR that explains why the mechanism is necessary and
how it remains compatible with Native Image.

## Project-Specific Rules

| Rule | Rationale | Enforced scope | Allowed exceptions | Evidence |
|---|---|---|---|---|
| Only `corba-omg-api` may define `org.omg.*` packages. | Legacy API ownership must be explicit and isolated. | Repository Java sources. | None without ADR. | ArchUnit source-tree check. |
| IDL and codegen modules must not depend on transport or protocol runtime packages. | IDL compilation and source generation must stay syntax/semantic focused. | `codegen`, `idl`, and `idlj` packages. | None without ADR. | ArchUnit dependency rules. |
| Protocol modules must not depend on ORB core or POA. | CDR/GIOP/IIOP/IOR are lower layers. | Protocol packages. | None without ADR. | ArchUnit dependency rules. |
| `corba-cdr` must not depend on GIOP, IIOP, ORB, POA, or services. | CDR is the primitive encoding layer. | CDR packages. | None without ADR. | ArchUnit dependency rules. |
| `corba-giop` and `corba-iiop` must not depend upward into ORB core, POA, services, or IDL compiler packages. | Protocol framing and transport stay independent of runtime dispatch and compiler behavior. | GIOP and IIOP packages. | None without ADR. | ArchUnit dependency rules. |
| `corba-common` must not import feature modules. | Shared diagnostics and limits remain reusable by every layer. | `corba-common` main source. | None without ADR. | Source-tree check. |
| Generated Java source must remain independent of CORBA runtime artifacts not in the minimal mapping slice. | The first source-generation slice is compile-safe and self-contained. | Codegen generated-source tests. | Later roadmap tasks may add documented generated artifacts. | Generated-source token checks. |

## GraalVM Native Image Rules

| Rule | Rationale | Enforced scope | Allowed exceptions | Evidence |
|---|---|---|---|---|
| Production paths must not use reflection, dynamic proxies, `java.lang.invoke`, `ServiceLoader`, `ClassLoader`, or classpath-scanning libraries. | These mechanisms often require reachability metadata and hide runtime discovery. | Production main classes and generated source. | Test harnesses only; production use requires ADR. | ArchUnit dependency rules and generated-source token checks. |
| Production paths must not use runtime bytecode generation or instrumentation APIs. | Runtime generation is hard to analyze and hostile to static images. | Normal runtime packages and generated source. | None without ADR. | ArchUnit dependency rules. |
| Production paths must not use Java serialization streams, `Externalizable`, serialization hooks, or new explicit `Serializable` declarations. | Java serialization is metadata-heavy and unnecessary for normal CORBA marshaling. | Production main source and generated source. | None without ADR. | ArchUnit dependency rules and source-token checks. |
| Production paths must not use `Unsafe`, `sun.*`, `jdk.internal.*`, or security-manager-era APIs. | Internal APIs are brittle across JDKs and Native Image configurations. | Production main classes and generated source. | None without ADR. | ArchUnit dependency rules and generated-source token checks. |
| Resource and class discovery must be explicit. | Native images need intentional reachability. | Production main code. | Test resources and build tooling are out of scope. | `ClassLoader` and scanning-library bans. |

## General Java Baseline Rules

| Rule | Rationale | Enforced scope | Allowed exceptions | Evidence |
|---|---|---|---|---|
| No `ObjectInputStream`, `ObjectOutputStream`, `Externalizable`, or new explicit `Serializable` in main code. | Serialization should be a deliberate design decision, not an accidental dependency. | Production main source. | None without ADR. | ArchUnit dependency rules and source-token checks. |
| No finalizers. | Finalization is deprecated and nondeterministic. | Production main classes and generated source. | None. | ArchUnit method rule and generated-source token checks. |
| No direct `System.exit` outside CLI entrypoint wrappers. | Libraries must report errors rather than terminating host processes. | Production main classes. | CLI `main` wrappers only. | ArchUnit method-call rule. |
| No forced GC or process spawning. | These are host-environment controls and should not appear in library/runtime code. | Production main classes and generated source. | Future tooling tasks require documentation. | ArchUnit dependency/call rules and generated-source token checks. |
| No public static mutable fields. | Global mutable state makes behavior order-dependent. | Production main classes. | None without ADR. | ArchUnit field rule. |
| No direct internal JDK package use. | Internal APIs are not stable public contracts. | Production main classes and generated source. | None without ADR. | ArchUnit dependency rules and generated-source token checks. |
