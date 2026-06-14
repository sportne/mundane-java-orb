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
  - `namingServer` installs local `NameService`, binds and resolves names,
    resolves `corbaname:rir:`, and runs configured durable network Naming
    persistence through a restart-style local store check;
  - `iorDiagnostics` parses deterministic `corbaloc`, `corbaname`, and
    stringified IOR/profile values;
  - `interopReport` serializes and parses structured G6 interop reports.
  - `rmiIiop` exercises explicit RMI repository ID metadata, local loopback
    IIOP, `RmiIiopWireServerHandler`, `RmiIiopWireClient`, normal replies,
    empty user exceptions, unknown object keys, and unknown operations for the
    approved G7 RMI-IIOP slice.
  - `timeService` exercises G8-100 local Time Service UTC value creation,
    fixed-clock universal-time query, interval creation, bounded diagnostic
    rejection, and G8-110 loopback IIOP/Naming exposure.
  - `eventService` exercises G8-250 local Event Service channel creation, push
    and pull delivery, bounded rejection, loopback IIOP/Naming exposure, and
    clean shutdown.
  - `notificationService` exercises G8-370 local Notification Service channel
    creation, structured event validation, filter validation, QoS rejection,
    local delivery, loopback IIOP/Naming exposure, and clean shutdown.
  - `tradingService` exercises G8-470 local Trading Service type and offer
    registration, bounded constraint rejection, local query, import/export
    disabled diagnostics, loopback IIOP/Naming exposure, and clean shutdown.
  - `interopClient` composes the approved local client-side interop smoke
    entrypoints, including IDL validation, generated client invocation, IOR
    diagnostics, structured report parsing, G12 broad IDL validation, and
    RMI-IIOP loopback behavior.
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

## G12-050 broad IDL Native Image evidence

G12-050 extends the validation smoke path used by the aggregate interop client
and server binaries with a compact G12 broad-IDL fixture containing native
declarations, value boxes, abstract valuetypes, valuetype inheritance, supported
interfaces, factories, and repository-prefix metadata. The interop report CLI
also records deterministic Native Image missing-prerequisite reports for
`g12-wide-valuetypes` through the existing `native-lane-report` command. The
accepted Native Image metadata set remains empty.

## G12-140 Naming persistence Native Image evidence

G12-140 extends the `namingServer` smoke entrypoint with caller-configured
durable network Naming persistence. The smoke creates `NamingPersistenceOptions`
with a durable `OrbIdentity`, writes a bounded `MJNS` store to a temporary
directory, binds a durable IOR, restarts the local Naming endpoint with the same
store and endpoint, and resolves the value through `corbaname`. The path uses
ordinary file I/O, bounded codecs, and explicit constructors only; it introduces
no reflection metadata, dynamic proxies, Java serialization metadata,
service-loader discovery, runtime bytecode generation, internal JDK APIs, or
`Unsafe`.

G13-010 adds forked-JVM restart coverage in the normal unit-test lane without
changing Native Image smoke entrypoints. The existing native smoke remains the
Native Image parity check for the durable Naming persistence API surface.

## G8-100/G8-110 Time Service Native Image evidence

G8-100 adds a `timeService` aggregate smoke target covering the local Time
Service value and clock-query surface. The smoke uses explicit constructors,
`java.time.Clock`, immutable values, and deterministic exception diagnostics;
G8-110 extends the same target with descriptor-backed loopback IIOP calls and
Naming-resolved Time Service IOR calls. G8-140 reruns the SDKMAN GraalVM native
smoke and records live peer clients invoking the Native Image Time Service
server successfully across JacORB, GlassFish CORBA ORB, and JBoss OpenJDK ORB.
It introduces no reflection metadata, dynamic proxies, Java serialization
metadata, service-loader discovery, runtime bytecode generation, process
execution, internal JDK APIs, or `Unsafe`.

## G8-250/G8-270 Event Service Native Image evidence

G8-250 adds an `eventService` aggregate smoke target covering the implemented
local and loopback Event Service subset. The smoke uses explicit constructors,
bounded local options, primitive project `AnyValue<?>` payloads,
descriptor-backed IIOP codecs, and optional Naming registration to exercise
channel creation, push delivery, pull delivery, bounded rejection, loopback
IIOP/Naming exposure, and clean shutdown. It introduces no reflection metadata,
dynamic proxies, Java serialization metadata, service-loader discovery, runtime
bytecode generation, process execution, internal JDK APIs, or `Unsafe`. G8-270
closes the Event Service conformance record using that smoke evidence together
with the local unit, loopback IIOP/Naming, and dry-run interop metadata lanes;
it adds no new Native Image metadata or live peer execution.

## G8-370 Notification Service Native Image evidence

G8-370 adds a `notificationService` aggregate smoke target covering the
implemented local and loopback Notification Service subset. The smoke uses
explicit constructors, immutable structured-event values, bounded filter and
policy validators, descriptor-backed IIOP codecs, and optional Naming
registration to exercise channel creation, structured-event validation, filter
validation, QoS rejection, local delivery, loopback IIOP/Naming exposure, and
clean shutdown. The source-level Native Image audit now includes
`modules/corba-notification-service/src/main`. The accepted metadata set
remains empty: no reflection metadata, dynamic proxies, Java serialization
metadata, service-loader discovery, runtime bytecode generation, process
execution, internal JDK APIs, or `Unsafe` are introduced.

G8-380 adds Notification Service interop metadata only. The `notification-service`
dry-run and missing-prerequisite report paths reuse existing JVM/native lane
inputs and do not add Native Image metadata files, class-initialization
overrides, reflection, dynamic proxies, Java serialization metadata,
service-loader discovery, runtime bytecode generation, internal JDK APIs, or
`Unsafe`.

G8-390 closes the Notification Service conformance record using that smoke
evidence together with the local unit, loopback IIOP/Naming, and dry-run
interop metadata lanes. It adds no new Native Image metadata or live peer
execution.

## G8-470 Trading Service Native Image evidence

G8-470 adds a `tradingService` aggregate smoke target covering the implemented
local and loopback Trading Service subset. The smoke uses explicit
constructors, bounded type and offer values, the closed-world constraint
parser/evaluator, descriptor-backed IIOP codecs, and optional Naming
registration to exercise type registration, offer registration, constraint
rejection, local query, import/export disabled diagnostics, loopback
IIOP/Naming exposure, and clean shutdown. The source-level Native Image audit
now includes `modules/corba-trading-service/src/main`. The accepted metadata
set remains empty: no reflection metadata, dynamic proxy metadata, Java
serialization metadata, service-loader discovery, runtime bytecode generation,
scripting engines, process execution in production sources, internal JDK APIs,
or `Unsafe` are introduced.

G8-490 closes the Trading Service conformance record with this Native Image
evidence as the closed-world execution proof for the implemented local/IIOP
subset. The closure does not add reflection metadata, dynamic proxies, Java
serialization metadata, service-loader discovery, runtime bytecode generation,
or live peer execution claims.

## G13-060 durable POA registry Native Image evidence

G13-060 extends the `generatedServer` smoke entrypoint with durable POA path
registry registration, duplicate rejection, transient-ORB rejection, durable-key
approval lookup, and shutdown rejection. The smoke uses explicit constructors
and bounded durable-key APIs only; it introduces no reflection metadata, dynamic
proxies, Java serialization metadata, service-loader discovery, runtime bytecode
generation, internal JDK APIs, or `Unsafe`.

## G13-070 durable POA activation Native Image evidence

G13-070 extends the `generatedServer` smoke entrypoint with registered durable
POA activation lookup. The smoke creates a durable ORB, registers a missing
persistent child path, installs an explicit `PoaAdapterActivator`, resolves a
decoded durable key through the root POA, and verifies unregistered paths fail
without activation. The path remains explicit-constructor only and introduces no
reflection metadata, dynamic proxies, Java serialization metadata,
service-loader discovery, runtime bytecode generation, internal JDK APIs, or
`Unsafe`.

## G13-080 durable POA rehydration Native Image evidence

G13-080 extends the `generatedServer` smoke entrypoint with public durable
reference lookup through a retained `ServantActivator`. The smoke creates a
durable ORB and persistent POA, registers the POA path, recreates an explicit
reference template, resolves the reference from its `MJOK` durable key, and
invokes the generated-style dispatcher through `LocalOrb`. The path remains
explicit-constructor only and introduces no reflection metadata, dynamic
proxies, Java serialization metadata, service-loader discovery, runtime
bytecode generation, internal JDK APIs, or `Unsafe`.

## G13-090 durable IIOP resolver Native Image evidence

G13-090 extends the `generatedServer` smoke entrypoint with
`IiopOrbServerHandler` durable resolver dispatch. The smoke builds a handler
with descriptor-level operation bindings, sends a GIOP request carrying opaque
durable object-key bytes, resolves those bytes through the POA-owned
rehydration path, and decodes the normal reply. The IIOP module does not parse
`MJOK` or depend on `corba-poa`; the path remains explicit-constructor only and
introduces no reflection metadata, dynamic proxies, Java serialization
metadata, service-loader discovery, runtime bytecode generation, internal JDK
APIs, or `Unsafe`.
