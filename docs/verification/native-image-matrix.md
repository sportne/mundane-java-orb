# Native Image Matrix

## Toolchains

- GraalVM JDK 21 Native Image.
- GraalVM JDK 25 Native Image.

Native Image checks should discover SDKMAN-installed GraalVM candidates before
falling back to a bare `native-image` command on `PATH`. Prefer explicit
`NATIVE_IMAGE`, then `JAVA_HOME/bin/native-image`, then SDKMAN candidates under
`$SDKMAN_CANDIDATES_DIR/java` or `$HOME/.sdkman/candidates/java`. When a SDKMAN
candidate is used, run Gradle with `JAVA_HOME` set to that candidate and
`$JAVA_HOME/bin` prepended to `PATH`.

## Required native binaries

- CDR primitive test binary.
- `corba-idlj`.
- generated client sample.
- generated server sample.
- naming server.
- IOR diagnostic tool.
- selected interop clients/servers.

## Required test levels

- smoke;
- integration;
- interop;
- startup/shutdown;
- class-initialization audit;
- reflection metadata audit.

## Current native checks

- `:modules:corba-native-image:nativeImageBinariesSmoke` builds and executes
  the aggregate Native Image smoke suite:
  - `idljValidate` validates compact valid and invalid IDL through `IdljCli`;
  - `generatedClient` exercises generated-style local client invocation through
    `LocalOrb`;
  - `generatedServer` exercises generated-style servant/dispatcher invocation
    through `LocalOrb`;
  - `namingServer` installs local `NameService`, binds and resolves names, and
    resolves `corbaname:rir:`;
  - `iorDiagnostics` parses deterministic `corbaloc`, `corbaname`, and
    stringified IOR/profile values;
  - `interopReport` serializes and parses structured G6 interop reports.
  - `rmiIiop` exercises explicit RMI repository ID metadata, local loopback
    IIOP, `RmiIiopWireServerHandler`, `RmiIiopWireClient`, normal replies,
    empty user exceptions, unknown object keys, and unknown operations for the
    approved G7 RMI-IIOP slice.
  - `interopClient` composes the approved local client-side interop smoke
    entrypoints, including IDL validation, generated client invocation, IOR
    diagnostics, structured report parsing, and RMI-IIOP loopback behavior.
  - `interopServer` composes the approved local server-side interop smoke
    entrypoints, including generated server dispatch, local naming,
    structured report parsing, and RMI-IIOP loopback behavior.
- `:modules:corba-cdr:nativeCdrSmoke` builds and executes a GraalVM Native
  Image smoke executable for the CDR primitive, string, sequence, and
  encapsulation reader/writer API.
- `:modules:corba-idl-semantics:nativeIdlSemanticsSmoke` builds and executes a
  GraalVM Native Image smoke executable for parser-to-semantics behavior over a
  compact IDL fixture.
- `:modules:corba-idlj-cli:nativeIdljValidateSmoke` builds and executes a
  GraalVM Native Image smoke executable for the validation-only `corba-idlj`
  command path over compact valid and invalid IDL fixtures.
- `:modules:corba-typecode:nativeTypecodeDescriptorSmoke` builds and executes a
  GraalVM Native Image smoke executable for static descriptor construction and
  compile-only codec failure behavior.

## G6-910 class-initialization and metadata policy

G6-910 uses default GraalVM class-initialization behavior for the smoke binaries.
No runtime class-initialization override is required by the local descriptor,
ORB, naming, IOR, IDL validation, or interop-report paths covered here.

The accepted metadata set is empty for:

- reflection configuration;
- dynamic proxy configuration;
- Java serialization configuration;
- service-loader discovery;
- runtime bytecode generation.

Any future metadata file must be committed as reviewed source and added to this
matrix with the owning task and test evidence.

## G6-930 closure evidence

G6-930 keeps Native Image hardening verification source-level and deterministic
by checking that native-image sources do not introduce reflection metadata,
dynamic proxies, service-loader discovery, serialization metadata, runtime
bytecode generation, process execution, internal JDK APIs, or `Unsafe`. The JVM
test lane also repeats representative smoke entrypoints before optional Native
Image compilation.

## G7-100 RMI-IIOP closure evidence

G7-100 extends the source-level Native Image audit to the RMI-IIOP production
sources and the new RMI-IIOP native smoke entrypoint. The accepted metadata set
remains empty: no reflection, dynamic proxy, Java serialization, service-loader,
runtime bytecode generation, or class-initialization override metadata is needed
for the implemented local RMI-IIOP slice.

The optional single-smoke command is:

```bash
./gradlew :modules:corba-native-image:nativeRmiIiopSmoke
```

## G10-020 IDL-to-Java closure evidence

G10-020 keeps the expanded legacy IDL-to-Java source surface closed-world and
source-level auditable. Generated helpers, holders, abstract stubs, abstract POA
placeholders, alias markers, union value classes, and descriptor/codec sources
compile without reflection metadata, dynamic proxies, Java serialization
metadata, service-loader discovery, runtime bytecode generation, process
execution, internal JDK APIs, `Unsafe`, or `org.omg.*` dependencies. No Native
Image metadata files are introduced by this task.

## G10-030 OMG compatibility API evidence

G10-030 keeps `corba-omg-api` as a source-level compatibility artifact. The
expanded `org.omg.*` and `org.omg.CosNaming` surfaces compile representative
generated-style sources without reflection, dynamic proxies, classpath scanning,
service-loader discovery, Java serialization API use, runtime bytecode
generation, process execution, internal JDK APIs, or `Unsafe`. No Native Image
metadata files or runtime discovery hooks are introduced by this task.

## G10-040 wire closure evidence

G10-040 keeps CDR, GIOP, IIOP, IOR, Any, and TypeCode closure at explicit
codec/framing boundaries. The new wire TypeCode, wire Any, target-address,
exception-body, code-set, TLS component, and fragment-assembly helpers use
ordinary constructors and bounded CDR APIs only. No reflection metadata, dynamic
proxy metadata, Java serialization metadata, service-loader discovery, runtime
bytecode generation, process execution, internal JDK APIs, or `Unsafe` are
introduced.

## G10-050 network dispatch evidence

G10-050 keeps the ORB/POA IIOP bridge explicit and closed-world friendly.
`IiopOrbClient`, `IiopOrbServerHandler`, `IiopObjectReference`, and
`IiopInvocationCodec` use static operation descriptors, caller-supplied codecs,
existing bounded IIOP/GIOP entrypoints, and direct `LocalOrb` invocation. The
loopback evidence in `IiopOrbDispatchTest` covers KeyAddr, ProfileAddr, and
ReferenceAddr routing, normal replies, declared user-exception replies, unknown
object keys, and system-exception replies. No reflection metadata, dynamic proxy
metadata, Java serialization metadata, service-loader discovery, runtime
bytecode generation, process execution, internal JDK APIs, or `Unsafe` are
introduced.

## G10-080 Portable Interceptor evidence

G10-080 keeps Portable Interceptor request-flow behavior source-level and
closed-world friendly. Interceptors are caller-supplied objects registered
through `PortableInterceptorRegistry`; no classpath scanning, service-loader
discovery, reflection dispatch, dynamic proxies, runtime bytecode generation,
Java serialization metadata, process execution, internal JDK APIs, or `Unsafe`
are introduced. The local evidence covers deterministic callback ordering,
service-context propagation over the ORB/IIOP loopback path, duplicate
registration diagnostics, and callback failure reporting.

## G10-090 RMI-IIOP compatibility evidence

G10-090 keeps the expanded RMI-IIOP compatibility surface explicit and
closed-world friendly. Sequence payloads, remote object-reference keys,
declared-value member metadata, user-exception payload fields, and
remote-interface inheritance are represented by immutable model records,
generated binding metadata, and bounded local CDR/GIOP/IIOP codecs. No
reflection metadata, dynamic proxies, Java serialization metadata, classpath
scanning, service-loader discovery, runtime bytecode generation, process
execution, internal JDK APIs, or `Unsafe` are introduced.

## G10-100 native interop binary evidence

G10-100 adds aggregate Native Image interop client and server smoke binaries for
the completed local G10 lanes. The binaries are built and run by
`:modules:corba-native-image:nativeImageBinariesSmoke` when Native Image is
available. JVM parity tests execute the same entrypoints repeatedly, and the
interop CLI records structured `infrastructure-failure` reports when requested
native client or server binaries are absent. No reflection metadata, dynamic
proxy metadata, Java serialization metadata, classpath scanning,
service-loader discovery, runtime bytecode generation, process execution in
production sources, internal JDK APIs, or `Unsafe` are introduced.
