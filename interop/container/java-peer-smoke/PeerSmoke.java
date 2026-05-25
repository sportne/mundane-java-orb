import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.omg.CORBA.ARG_IN;
import org.omg.CORBA.Any;
import org.omg.CORBA.ExceptionList;
import org.omg.CORBA.NVList;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Request;
import org.omg.CORBA.ServerRequest;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.PortableServer.DynamicImplementation;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public final class PeerSmoke {
  private static final String BASIC_REPOSITORY_ID = "IDL:interop/basic/Smoke:1.0";
  private static final String LEGACY_REPOSITORY_ID = "IDL:interop/Smoke:1.0";
  private static final String CALCULATOR_REPOSITORY_ID = "IDL:example/calc/Calculator:1.0";
  private static final String PROBLEM_REPOSITORY_ID = "IDL:example/calc/CalculatorProblem:1.0";

  private PeerSmoke() {}

  public static void main(String[] args) throws Exception {
    String role = System.getenv().getOrDefault("INTEROP_ROLE", args.length == 0 ? "report" : args[0]);
    switch (role) {
      case "server" -> server();
      case "client" -> client();
      case "health" -> health();
      case "naming" -> naming();
      case "report" -> report();
      default -> throw new IllegalArgumentException("unknown role: " + role);
    }
  }

  private static void server() throws Exception {
    requireScenarioIdl();
    ORB orb = orb(true);
    POA root = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    root.the_POAManager().activate();
    org.omg.CORBA.Object ref =
        root.servant_to_reference(
            "rmi-iiop".equals(scenario()) ? new CalculatorServant(orb) : new SmokeServant(orb));
    Path ior = iorPath("server");
    Files.createDirectories(ior.getParent());
    Files.writeString(ior, orb.object_to_string(ref) + System.lineSeparator());
    System.out.println("peer smoke server ready: " + ior);
    orb.run();
  }

  private static void client() throws Exception {
    requireScenarioIdl();
    ORB orb = orb(false);
    Path ior = iorPath("server");
    String value = Files.readString(ior).trim();
    org.omg.CORBA.Object ref = orb.string_to_object(value);
    if ("rmi-iiop".equals(scenario())) {
      verifyCalculator(orb, ref);
      return;
    }
    boolean exists = !ref._non_existent();
    boolean isSmoke = ref._is_a(BASIC_REPOSITORY_ID) || ref._is_a(LEGACY_REPOSITORY_ID);
    if (!exists || !isSmoke) {
      throw new IllegalStateException(
          "unexpected peer object state: exists=" + exists + ", isSmoke=" + isSmoke);
    }
    System.out.println("peer smoke client completed");
  }

  private static void health() throws Exception {
    Path ior = iorPath("server");
    if (!Files.isRegularFile(ior) || Files.readString(ior).trim().isEmpty()) {
      throw new IllegalStateException("server IOR not ready: " + ior);
    }
    System.out.println("peer smoke health ok");
  }

  private static void naming() throws Exception {
    requireScenarioIdl();
    ORB orb = orb(true);
    Path ior = iorPath("naming");
    Files.createDirectories(ior.getParent());
    Files.writeString(
        ior,
        orb.object_to_string(orb.resolve_initial_references("RootPOA")) + System.lineSeparator());
    System.out.println("peer smoke naming wrote placeholder IOR: " + ior);
  }

  private static void report() throws IOException {
    Files.createDirectories(Path.of("/interop/reports"));
    Files.writeString(Path.of("/interop/reports/peer-smoke-report.txt"), "peer smoke report\n");
    System.out.println("peer smoke report completed");
  }

  private static ORB orb(boolean server) {
    Properties properties = new Properties();
    String orbClass = System.getenv("INTEROP_ORB_CLASS");
    String singleton = System.getenv("INTEROP_ORB_SINGLETON_CLASS");
    if (orbClass != null && !orbClass.isBlank()) {
      properties.setProperty("org.omg.CORBA.ORBClass", orbClass);
    }
    if (singleton != null && !singleton.isBlank()) {
      properties.setProperty("org.omg.CORBA.ORBSingletonClass", singleton);
    }
    String[] args = new String[0];
    if (server) {
      putDefault(properties, "OAPort", "2809");
      putDefault(properties, "OAIAddr", "0.0.0.0");
      putDefault(properties, "jacorb.ior_proxy_host", "host.docker.internal");
      putDefault(properties, "jacorb.ior_proxy_port", "2809");
      putDefault(properties, "com.sun.CORBA.ORBServerHost", "0.0.0.0");
      putDefault(properties, "com.sun.CORBA.ORBServerPort", "2809");
      putDefault(properties, "com.sun.CORBA.transport.ORBListenSocket", "IIOP_CLEAR_TEXT:2809");
      args =
          new String[] {
            "-ORBServerHost", "0.0.0.0",
            "-ORBServerPort", "2809"
          };
    }
    return ORB.init(args, properties);
  }

  private static void putDefault(Properties properties, String key, String value) {
    String override = System.getenv("INTEROP_ORB_PROPERTY_" + key.replace('.', '_'));
    properties.setProperty(key, override == null || override.isBlank() ? value : override);
  }

  private static void requireScenarioIdl() throws IOException {
    Path idl = Path.of("/interop/scenario.idl");
    if (!Files.isRegularFile(idl) || Files.readString(idl).isBlank()) {
      throw new IllegalStateException("scenario IDL is missing or empty: " + idl);
    }
  }

  private static Path iorPath(String role) {
    return Path.of("/interop/iors", scenario() + "-" + role + ".ior");
  }

  private static String scenario() {
    return System.getenv().getOrDefault("INTEROP_SCENARIO", "manual");
  }

  private static void verifyCalculator(ORB orb, org.omg.CORBA.Object ref) {
    Request add = ref._request("add");
    add.add_in_arg().insert_long(13);
    add.add_in_arg().insert_long(29);
    add.set_return_type(orb.get_primitive_tc(TCKind.tk_long));
    add.invoke();
    if (add.return_value().extract_long() != 42) {
      throw new IllegalStateException("Calculator.add returned an unexpected value");
    }

    Request describe = ref._request("describe");
    describe.add_in_arg().insert_wstring("Ada");
    describe.set_return_type(orb.create_wstring_tc(0));
    describe.invoke();
    if (!"Calculator Ada".equals(describe.return_value().extract_wstring())) {
      throw new IllegalStateException("Calculator.describe returned an unexpected value");
    }

    Request clear = ref._request("clear");
    clear.set_return_type(orb.get_primitive_tc(TCKind.tk_void));
    clear.invoke();
    System.out.println("peer smoke calculator client completed");
  }

  private static final class SmokeServant extends DynamicImplementation {
    private final ORB orb;

    private SmokeServant(ORB orb) {
      this.orb = orb;
    }

    @Override
    public void invoke(ServerRequest request) {
      String operation = request.operation();
      if ("_is_a".equals(operation)) {
        NVList arguments = orb.create_list(0);
        Any repositoryId = orb.create_any();
        repositoryId.type(orb.get_primitive_tc(TCKind.tk_string));
        arguments.add_value("repository_id", repositoryId, ARG_IN.value);
        request.arguments(arguments);
        String requested = repositoryId.extract_string();
        Any result = orb.create_any();
        result.insert_boolean(true);
        if (!BASIC_REPOSITORY_ID.equals(requested) && !LEGACY_REPOSITORY_ID.equals(requested)) {
          result = orb.create_any();
          result.insert_boolean(false);
        }
        request.set_result(result);
        return;
      }
      if ("_non_existent".equals(operation)) {
        Any result = orb.create_any();
        result.insert_boolean(false);
        request.set_result(result);
        return;
      }
      Any result = orb.create_any();
      result.insert_string("ok");
      request.set_result(result);
    }

    @Override
    public String[] _all_interfaces(POA poa, byte[] objectId) {
      return new String[] {BASIC_REPOSITORY_ID, LEGACY_REPOSITORY_ID};
    }
  }

  private static final class CalculatorServant extends DynamicImplementation {
    private final ORB orb;

    private CalculatorServant(ORB orb) {
      this.orb = orb;
    }

    @Override
    public void invoke(ServerRequest request) {
      switch (request.operation()) {
        case "add" -> add(request);
        case "describe" -> describe(request);
        case "clear" -> clear(request);
        case "_is_a" -> isA(request);
        case "_non_existent" -> nonExistent(request);
        default -> throw new org.omg.CORBA.BAD_OPERATION(request.operation());
      }
    }

    private void add(ServerRequest request) {
      NVList arguments = orb.create_list(0);
      Any left = orb.create_any();
      left.type(orb.get_primitive_tc(TCKind.tk_long));
      Any right = orb.create_any();
      right.type(orb.get_primitive_tc(TCKind.tk_long));
      arguments.add_value("left", left, ARG_IN.value);
      arguments.add_value("right", right, ARG_IN.value);
      request.arguments(arguments);
      Any result = orb.create_any();
      result.insert_long(left.extract_long() + right.extract_long());
      request.set_result(result);
    }

    private void describe(ServerRequest request) {
      NVList arguments = orb.create_list(0);
      Any name = orb.create_any();
      name.type(orb.create_wstring_tc(0));
      arguments.add_value("name", name, ARG_IN.value);
      request.arguments(arguments);
      String value = name.extract_wstring();
      if ("bad".equals(value)) {
        Any problem = orb.create_any();
        problem.type(problemType());
        request.set_exception(problem);
        return;
      }
      Any result = orb.create_any();
      result.insert_wstring("Calculator " + value);
      request.set_result(result);
    }

    private void clear(ServerRequest request) {
      Any result = orb.create_any();
      result.type(orb.get_primitive_tc(TCKind.tk_void));
      request.set_result(result);
    }

    private void isA(ServerRequest request) {
      NVList arguments = orb.create_list(0);
      Any repositoryId = orb.create_any();
      repositoryId.type(orb.get_primitive_tc(TCKind.tk_string));
      arguments.add_value("repository_id", repositoryId, ARG_IN.value);
      request.arguments(arguments);
      Any result = orb.create_any();
      result.insert_boolean(CALCULATOR_REPOSITORY_ID.equals(repositoryId.extract_string()));
      request.set_result(result);
    }

    private void nonExistent(ServerRequest request) {
      Any result = orb.create_any();
      result.insert_boolean(false);
      request.set_result(result);
    }

    private TypeCode problemType() {
      return orb.create_exception_tc(
          PROBLEM_REPOSITORY_ID, "CalculatorProblem", new StructMember[0]);
    }

    @Override
    public String[] _all_interfaces(POA poa, byte[] objectId) {
      return new String[] {CALCULATOR_REPOSITORY_ID};
    }
  }
}
