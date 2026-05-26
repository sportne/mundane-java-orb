package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.iiop.IiopClient;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopServer;
import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.IiopVersion;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.ObjectKey;
import io.github.mundanej.mjo.ior.StringifiedIor;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.rmi.iiop.RmiCdrValue;
import io.github.mundanej.mjo.rmi.iiop.RmiIdlExceptionReference;
import io.github.mundanej.mjo.rmi.iiop.RmiIdlInterface;
import io.github.mundanej.mjo.rmi.iiop.RmiIdlOperation;
import io.github.mundanej.mjo.rmi.iiop.RmiIdlParameter;
import io.github.mundanej.mjo.rmi.iiop.RmiIdlTypeReference;
import io.github.mundanej.mjo.rmi.iiop.RmiIiopObjectKey;
import io.github.mundanej.mjo.rmi.iiop.RmiIiopUserExceptionPayload;
import io.github.mundanej.mjo.rmi.iiop.RmiIiopWireClient;
import io.github.mundanej.mjo.rmi.iiop.RmiIiopWireServerHandler;
import io.github.mundanej.mjo.rmi.iiop.RmiIiopWireUserException;
import io.github.mundanej.mjo.rmi.iiop.RmiRepositoryIdPlan;
import io.github.mundanej.mjo.rmi.iiop.RmiRepositoryIdValue;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/** Local JVM/Native Image lane commands used by the live G10-120 direction matrix. */
public final class LiveInteropLane {

  private static final String BASIC_REPOSITORY_ID = "IDL:interop/basic/Smoke:1.0";
  private static final String LEGACY_SMOKE_REPOSITORY_ID = "IDL:interop/Smoke:1.0";
  private static final byte[] BASIC_OBJECT_KEY =
      "mjo-basic-smoke".getBytes(StandardCharsets.US_ASCII);
  private static final String CALCULATOR_REPOSITORY_ID = "IDL:example/calc/Calculator:1.0";
  private static final String PROBLEM_REPOSITORY_ID = "IDL:example/calc/CalculatorProblem:1.0";
  private static final IdlTypeReference LONG_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", "int", Optional.empty());
  private static final IdlTypeReference WSTRING_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "wstring", "java.lang.String", Optional.empty());
  private static final IdlTypeReference VOID_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "void", "void", Optional.empty());
  private static final IdlTypeReference PROBLEM_TYPE =
      new IdlTypeReference(
          IdlTypeKind.EXCEPTION,
          "::example::calc::CalculatorProblem",
          CalculatorProblem.class.getName(),
          Optional.of(RepositoryId.parse(PROBLEM_REPOSITORY_ID)));
  private static final IdlOperationDescriptor ADD_DESCRIPTOR =
      new IdlOperationDescriptor(
          "add",
          LONG_TYPE,
          List.of(
              new IdlParameterDescriptor("left", IdlParameterMode.IN, LONG_TYPE),
              new IdlParameterDescriptor("right", IdlParameterMode.IN, LONG_TYPE)),
          List.of());
  private static final IdlOperationDescriptor DESCRIBE_DESCRIPTOR =
      new IdlOperationDescriptor(
          "describe",
          WSTRING_TYPE,
          List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, WSTRING_TYPE)),
          List.of(PROBLEM_TYPE));
  private static final IdlOperationDescriptor CLEAR_DESCRIPTOR =
      new IdlOperationDescriptor("clear", VOID_TYPE, List.of(), List.of());
  private static final IdlGeneratedTypeDescriptor CALCULATOR_DESCRIPTOR =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::example::calc::Calculator",
          Calculator.class.getName(),
          RepositoryId.parse(CALCULATOR_REPOSITORY_ID),
          List.of(),
          List.of(),
          List.of(ADD_DESCRIPTOR, DESCRIBE_DESCRIPTOR, CLEAR_DESCRIPTOR));

  private LiveInteropLane() {}

  /** Returns whether the harness supplied a live interop lane environment. */
  public static boolean isEnabled(Map<String, String> env) {
    return env.containsKey("MJO_INTEROP_SERVER_IOR");
  }

  /** Executes one client lane against the IOR supplied by the harness. */
  public static void runClient(Map<String, String> env) throws Exception {
    String scenario = scenario(env);
    IiopObjectReference reference = endpointOverride(readReference(serverIorPath(env)), env);
    if ("rmi-iiop".equals(scenario)) {
      invokeCalculator(reference, env);
    } else {
      invokeObjectLiveness(reference);
    }
  }

  /** Starts one server lane and returns the running server for tests and harness wrappers. */
  public static RunningServer startServer(Map<String, String> env) throws IOException {
    String scenario = scenario(env);
    String bindHost = env.getOrDefault("MJO_INTEROP_BIND_HOST", "0.0.0.0");
    String advertisedHost = env.getOrDefault("MJO_INTEROP_ADVERTISE_HOST", "host.docker.internal");
    int port = Integer.parseInt(env.getOrDefault("MJO_INTEROP_PORT", "0"));
    if ("rmi-iiop".equals(scenario)) {
      return startCalculatorServer(bindHost, advertisedHost, port, serverIorPath(env));
    }
    return startLivenessServer(bindHost, advertisedHost, port, serverIorPath(env));
  }

  /** Verifies the legacy Java peer smoke repository ID against a running liveness server. */
  public static void assertLegacySmokeRepositoryId(Map<String, String> env) throws IOException {
    IiopObjectReference reference = endpointOverride(readReference(serverIorPath(env)), env);
    try (IiopClient client = IiopClient.connect(reference.endpoint(), IiopOptions.defaults())) {
      GiopReply reply =
          client.invoke(
              new GiopRequest(
                  GiopHeader.forType(GiopMessageType.REQUEST),
                  1L,
                  3,
                  reference.objectKey(),
                  "_is_a",
                  List.of(),
                  CdrWriter.bigEndian().writeString(LEGACY_SMOKE_REPOSITORY_ID).toByteArray()));
      SmokeAssertions.requireEquals(
          GiopReplyStatus.NO_EXCEPTION, reply.replyStatus(), "legacy smoke _is_a reply status");
      SmokeAssertions.requireEquals(
          true, reader(reply.header(), reply.body()).readBoolean(), "legacy smoke repository ID");
    }
  }

  /** Executes one server lane and blocks until the process is terminated by the harness. */
  public static void runServer(Map<String, String> env) throws Exception {
    RunningServer server = startServer(env);
    try {
      new CountDownLatch(1).await();
    } finally {
      server.close();
    }
  }

  private static RunningServer startLivenessServer(
      String bindHost, String advertisedHost, int port, Path serverIorPath) throws IOException {
    IiopServer server =
        IiopServer.bind(
            new IiopEndpoint(bindHost, port),
            IiopOptions.defaults(),
            LiveInteropLane::handleLiveness);
    IiopEndpoint advertised = new IiopEndpoint(advertisedHost, server.endpoint().port());
    Ior ior =
        new Ior(
            BASIC_REPOSITORY_ID,
            List.of(
                TaggedProfile.internetIop(
                    new IiopProfile(
                        IiopVersion.V1_2,
                        advertised.host(),
                        advertised.port(),
                        new ObjectKey(BASIC_OBJECT_KEY),
                        List.of()))));
    writeIor(serverIorPath, ior);
    return new RunningServer(server, null);
  }

  private static RunningServer startCalculatorServer(
      String bindHost, String advertisedHost, int port, Path serverIorPath) throws IOException {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<Calculator> reference =
        orb.bindWithObjectId(Calculator.class, CALCULATOR_DESCRIPTOR, "Calculator", dispatcher());
    RmiIiopObjectKey objectKey = RmiIiopObjectKey.forLocalObjectReference(reference);
    RmiIiopWireServerHandler handler =
        new RmiIiopWireServerHandler(orb, repositoryIdPlan())
            .register(objectKey, reference, rmiInterface());
    IiopServer server =
        IiopServer.bind(new IiopEndpoint(bindHost, port), IiopOptions.defaults(), handler);
    IiopObjectReference iiopReference =
        IiopObjectReference.fromLocal(
            new IiopEndpoint(advertisedHost, server.endpoint().port()), reference);
    writeIor(serverIorPath, iiopReference.ior());
    return new RunningServer(server, orb);
  }

  private static GiopReply handleLiveness(GiopRequest request) {
    if (!List.of("_non_existent", "_is_a").contains(request.operation())) {
      return new GiopReply(
          GiopHeader.forType(GiopMessageType.REPLY),
          request.requestId(),
          GiopReplyStatus.SYSTEM_EXCEPTION,
          List.of(),
          new byte[0]);
    }
    boolean result = false;
    if ("_is_a".equals(request.operation())) {
      CdrReader reader = reader(request.header(), request.body());
      String repositoryId = reader.readString();
      result =
          BASIC_REPOSITORY_ID.equals(repositoryId)
              || LEGACY_SMOKE_REPOSITORY_ID.equals(repositoryId);
    }
    byte[] body = CdrWriter.bigEndian().writeBoolean(result).toByteArray();
    return new GiopReply(
        GiopHeader.forType(GiopMessageType.REPLY),
        request.requestId(),
        GiopReplyStatus.NO_EXCEPTION,
        List.of(),
        body);
  }

  private static void invokeObjectLiveness(IiopObjectReference reference) {
    try (IiopClient client = IiopClient.connect(reference.endpoint(), IiopOptions.defaults())) {
      GiopReply reply =
          client.invoke(
              new GiopRequest(
                  GiopHeader.forType(GiopMessageType.REQUEST),
                  1L,
                  3,
                  reference.objectKey(),
                  "_non_existent",
                  List.of(),
                  new byte[0]));
      if (reply.replyStatus() != GiopReplyStatus.NO_EXCEPTION) {
        throw new IllegalStateException("liveness reply status was " + reply.replyStatus());
      }
      boolean nonExistent = reader(reply.header(), reply.body()).readBoolean();
      if (nonExistent) {
        throw new IllegalStateException("remote object reported non-existence");
      }
    }
  }

  private static void invokeCalculator(IiopObjectReference reference, Map<String, String> env)
      throws Exception {
    try (IiopClient client = IiopClient.connect(reference.endpoint(), IiopOptions.defaults());
        RmiIiopWireClient wireClient = new RmiIiopWireClient(client, repositoryIdPlan())) {
      RmiIiopObjectKey objectKey = RmiIiopObjectKey.fromBytes(reference.objectKey());
      RmiCdrValue add =
          wireClient.invoke(
              objectKey,
              addOperation(),
              List.of(RmiCdrValue.longValue(13), RmiCdrValue.longValue(29)));
      SmokeAssertions.requireEquals(RmiCdrValue.longValue(42), add, "live RMI-IIOP add result");
      RmiCdrValue describe =
          wireClient.invoke(
              objectKey, describeOperation(), List.of(RmiCdrValue.stringValue("Ada")));
      SmokeAssertions.requireEquals(
          RmiCdrValue.stringValue("Calculator Ada"), describe, "live RMI-IIOP describe result");
      assertCalculatorProblem(wireClient, objectKey);
      if (requiresRemoteClear(env)) {
        wireClient.invoke(objectKey, clearOperation(), List.of());
      }
    }
  }

  private static boolean requiresRemoteClear(Map<String, String> env) {
    String peer = env.getOrDefault("MJO_INTEROP_PEER", "");
    return !List.of("glassfish-orb", "jboss-openjdk-orb").contains(peer);
  }

  private static void assertCalculatorProblem(
      RmiIiopWireClient wireClient, RmiIiopObjectKey objectKey) throws Exception {
    try {
      wireClient.invoke(objectKey, describeOperation(), List.of(RmiCdrValue.stringValue("bad")));
      throw new AssertionError("live RMI-IIOP CalculatorProblem was not raised");
    } catch (RmiIiopWireUserException expected) {
      SmokeAssertions.requireEquals(
          PROBLEM_REPOSITORY_ID, expected.repositoryId(), "live RMI-IIOP exception repository ID");
    }
  }

  private static LocalInvocationDispatcher dispatcher() {
    return request -> {
      if (ADD_DESCRIPTOR.equals(request.operation())) {
        return (Integer) request.arguments().get(0) + (Integer) request.arguments().get(1);
      }
      if (DESCRIBE_DESCRIPTOR.equals(request.operation())) {
        String name = (String) request.arguments().getFirst();
        if ("bad".equals(name)) {
          throw new CalculatorProblem();
        }
        return "Calculator " + name;
      }
      if (CLEAR_DESCRIPTOR.equals(request.operation())) {
        return null;
      }
      throw new AssertionError("Unexpected operation " + request.operation().name());
    };
  }

  private static IiopObjectReference readReference(Path iorPath) throws IOException {
    return IiopObjectReference.fromIor(StringifiedIor.parse(Files.readString(iorPath).trim()));
  }

  private static IiopObjectReference endpointOverride(
      IiopObjectReference reference, Map<String, String> env) {
    String host = env.get("MJO_INTEROP_ENDPOINT_HOST_OVERRIDE");
    String port = env.get("MJO_INTEROP_ENDPOINT_PORT_OVERRIDE");
    if ((host == null || host.isBlank()) && (port == null || port.isBlank())) {
      return reference;
    }
    IiopEndpoint current = reference.endpoint();
    String endpointHost = host == null || host.isBlank() ? current.host() : host;
    int endpointPort = port == null || port.isBlank() ? current.port() : Integer.parseInt(port);
    return new IiopObjectReference(
        reference.ior(), new IiopEndpoint(endpointHost, endpointPort), reference.objectKey());
  }

  private static void writeIor(Path path, Ior ior) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(path, StringifiedIor.format(ior) + System.lineSeparator());
  }

  private static Path serverIorPath(Map<String, String> env) {
    return Path.of(required(env, "MJO_INTEROP_SERVER_IOR"));
  }

  private static String scenario(Map<String, String> env) {
    return env.getOrDefault("MJO_INTEROP_SCENARIO", "basic-idl");
  }

  private static String required(Map<String, String> env, String key) {
    String value = env.get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(key + " is required");
    }
    return value;
  }

  private static CdrReader reader(GiopHeader header, byte[] body) {
    return new CdrReader(
        header.littleEndian() ? CdrByteOrder.LITTLE_ENDIAN : CdrByteOrder.BIG_ENDIAN, body);
  }

  private static RmiIdlInterface rmiInterface() {
    return new RmiIdlInterface(
        "Calculator",
        "::example::calc::Calculator",
        Optional.of(Calculator.class.getName()),
        List.of(addOperation(), describeOperation(), clearOperation()));
  }

  private static RmiIdlOperation addOperation() {
    return new RmiIdlOperation(
        "add",
        RmiIdlTypeReference.builtin("long"),
        List.of(
            new RmiIdlParameter("left", RmiIdlTypeReference.builtin("long")),
            new RmiIdlParameter("right", RmiIdlTypeReference.builtin("long"))),
        List.of());
  }

  private static RmiIdlOperation describeOperation() {
    return new RmiIdlOperation(
        "describe",
        RmiIdlTypeReference.builtin("wstring"),
        List.of(new RmiIdlParameter("name", RmiIdlTypeReference.builtin("wstring"))),
        List.of(
            new RmiIdlExceptionReference(
                CalculatorProblem.class.getName(),
                "::example::calc::CalculatorProblem",
                List.of())));
  }

  private static RmiIdlOperation clearOperation() {
    return new RmiIdlOperation("clear", RmiIdlTypeReference.voidType(), List.of(), List.of());
  }

  private static RmiRepositoryIdPlan repositoryIdPlan() {
    return new RmiRepositoryIdPlan(
        List.of(
            new RmiRepositoryIdValue(Calculator.class.getName(), CALCULATOR_REPOSITORY_ID),
            new RmiRepositoryIdValue(CalculatorProblem.class.getName(), PROBLEM_REPOSITORY_ID)));
  }

  private interface Calculator {}

  private static final class CalculatorProblem extends Exception
      implements RmiIiopUserExceptionPayload {
    private static final long serialVersionUID = 1L;

    @Override
    public List<RmiCdrValue> rmiIiopFields() {
      return List.of();
    }
  }

  /** Running live-lane server resources. */
  public static final class RunningServer implements AutoCloseable {
    private final IiopServer server;
    private final LocalOrb orb;

    private RunningServer(IiopServer server, LocalOrb orb) {
      this.server = server;
      this.orb = orb;
    }

    @Override
    public void close() {
      server.close();
      if (orb != null) {
        orb.shutdown();
      }
    }
  }
}
