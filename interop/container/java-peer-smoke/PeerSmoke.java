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
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NameHelper;
import org.omg.PortableServer.DynamicImplementation;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public final class PeerSmoke {
  private static final String BASIC_REPOSITORY_ID = "IDL:interop/basic/Smoke:1.0";
  private static final String LEGACY_REPOSITORY_ID = "IDL:interop/Smoke:1.0";
  private static final String CALCULATOR_REPOSITORY_ID = "IDL:example/calc/Calculator:1.0";
  private static final String PROBLEM_REPOSITORY_ID = "IDL:example/calc/CalculatorProblem:1.0";
  private static final String TIME_SERVICE_REPOSITORY_ID = "IDL:omg.org/CosTime/TimeService:1.0";
  private static final String UTC_TIME_REPOSITORY_ID = "IDL:omg.org/TimeBase/UtcT:1.0";
  private static final String INTERVAL_REPOSITORY_ID = "IDL:omg.org/TimeBase/IntervalT:1.0";
  private static final long TIME_SERVICE_CURRENT_TICKS = 40_000_001L;
  private static final long TIME_SERVICE_CURRENT_INACCLO = 2L;
  private static final int TIME_SERVICE_CURRENT_INACCHI = 0;
  private static final short TIME_SERVICE_CURRENT_TDF = 0;
  private static final long TIME_SERVICE_EXPLICIT_TICKS = 1_234_567_890L;
  private static final long TIME_SERVICE_EXPLICIT_INACCLO = 7L;
  private static final int TIME_SERVICE_EXPLICIT_INACCHI = 256;
  private static final short TIME_SERVICE_EXPLICIT_TDF = -60;
  private static final long TIME_SERVICE_INTERVAL_LOWER = 7L;
  private static final long TIME_SERVICE_INTERVAL_UPPER = 12L;

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
            switch (scenario()) {
              case "rmi-iiop" -> new CalculatorServant(orb);
              case "time-service" -> new TimeServiceServant(orb);
              default -> new SmokeServant(orb);
            });
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
    org.omg.CORBA.Object ref =
        isDurableNamingScenario() ? resolveCorbanameTarget(orb, value) : orb.string_to_object(value);
    if ("rmi-iiop".equals(scenario())) {
      verifyCalculator(orb, ref);
      return;
    }
    if ("time-service".equals(scenario())) {
      verifyTimeService(orb, ref);
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

  private static org.omg.CORBA.Object resolveCorbanameTarget(ORB orb, String value) {
    if (!value.startsWith("corbaname:")) {
      throw new IllegalArgumentException("durable Naming scenario requires corbaname input");
    }
    int hash = value.indexOf('#');
    if (hash < 0 || hash == value.length() - 1) {
      throw new IllegalArgumentException("durable Naming corbaname must include a name path");
    }
    org.omg.CORBA.Object naming =
        orb.string_to_object("corbaloc:" + value.substring("corbaname:".length(), hash));
    Request resolve = naming._request("resolve");
    Any name = resolve.add_in_arg();
    NameHelper.insert(name, nameComponents(value.substring(hash + 1)));
    resolve.set_return_type(orb.get_primitive_tc(TCKind.tk_objref));
    resolve.invoke();
    if (resolve.env().exception() != null) {
      throw new IllegalStateException("durable Naming resolve failed", resolve.env().exception());
    }
    org.omg.CORBA.Object target = resolve.return_value().extract_Object();
    if (target == null) {
      throw new IllegalStateException("durable Naming resolve returned nil");
    }
    return target;
  }

  private static NameComponent[] nameComponents(String name) {
    String[] parts = name.split("/");
    NameComponent[] components = new NameComponent[parts.length];
    for (int index = 0; index < parts.length; index++) {
      if (parts[index].isBlank()) {
        throw new IllegalArgumentException("empty durable Naming component");
      }
      components[index] = new NameComponent(parts[index], "");
    }
    return components;
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
      if (!"jboss-openjdk-orb".equals(peer())) {
        putDefault(properties, "com.sun.CORBA.transport.ORBListenSocket", "IIOP_CLEAR_TEXT:2809");
      }
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

  private static boolean isDurableNamingScenario() {
    return "g13-durable-naming-peer-client-restart".equals(scenario());
  }

  private static String peer() {
    return System.getenv().getOrDefault("INTEROP_PEER", "manual");
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

  private static void verifyTimeService(ORB orb, org.omg.CORBA.Object ref) {
    Request universalTime = ref._request("universal_time");
    universalTime.set_return_type(utcType(orb));
    universalTime.invoke();
    assertNoException(universalTime, "universal_time");
    assertUtc(
        universalTime.return_value(),
        TIME_SERVICE_CURRENT_TICKS,
        TIME_SERVICE_CURRENT_INACCLO,
        TIME_SERVICE_CURRENT_INACCHI,
        TIME_SERVICE_CURRENT_TDF,
        "universal_time");

    Request newUniversalTime = ref._request("new_universal_time");
    putUnsignedLongLong(newUniversalTime.add_in_arg(), TIME_SERVICE_EXPLICIT_TICKS, orb);
    putUnsignedLong(newUniversalTime.add_in_arg(), TIME_SERVICE_EXPLICIT_INACCLO, orb);
    putUnsignedShort(newUniversalTime.add_in_arg(), TIME_SERVICE_EXPLICIT_INACCHI, orb);
    newUniversalTime.add_in_arg().insert_short(TIME_SERVICE_EXPLICIT_TDF);
    newUniversalTime.set_return_type(utcType(orb));
    newUniversalTime.invoke();
    assertNoException(newUniversalTime, "new_universal_time");
    assertUtc(
        newUniversalTime.return_value(),
        TIME_SERVICE_EXPLICIT_TICKS,
        TIME_SERVICE_EXPLICIT_INACCLO,
        TIME_SERVICE_EXPLICIT_INACCHI,
        TIME_SERVICE_EXPLICIT_TDF,
        "new_universal_time");

    Request newInterval = ref._request("new_interval");
    putUnsignedLongLong(newInterval.add_in_arg(), TIME_SERVICE_INTERVAL_LOWER, orb);
    putUnsignedLongLong(newInterval.add_in_arg(), TIME_SERVICE_INTERVAL_UPPER, orb);
    newInterval.set_return_type(intervalType(orb));
    newInterval.invoke();
    assertNoException(newInterval, "new_interval");
    assertInterval(newInterval.return_value(), TIME_SERVICE_INTERVAL_LOWER, TIME_SERVICE_INTERVAL_UPPER);
    System.out.println("peer smoke Time Service client completed");
  }

  private static void assertNoException(Request request, String operation) {
    if (request.env().exception() != null) {
      throw new IllegalStateException(operation + " failed", request.env().exception());
    }
  }

  private static void assertUtc(
      Any value, long time, long inacclo, int inacchi, short tdf, String operation) {
    org.omg.CORBA.portable.InputStream input = value.create_input_stream();
    long actualTime = input.read_ulonglong();
    long actualInacclo = input.read_ulong();
    int actualInacchi = input.read_ushort();
    short actualTdf = input.read_short();
    if (actualTime != time
        || actualInacclo != inacclo
        || actualInacchi != inacchi
        || actualTdf != tdf) {
      throw new IllegalStateException(
          operation
              + " returned unexpected UtcT: time="
              + actualTime
              + ", inacclo="
              + actualInacclo
              + ", inacchi="
              + actualInacchi
              + ", tdf="
              + actualTdf);
    }
  }

  private static void assertInterval(Any value, long lower, long upper) {
    org.omg.CORBA.portable.InputStream input = value.create_input_stream();
    long actualLower = input.read_ulonglong();
    long actualUpper = input.read_ulonglong();
    if (actualLower != lower || actualUpper != upper) {
      throw new IllegalStateException(
          "new_interval returned unexpected IntervalT: lower="
              + actualLower
              + ", upper="
              + actualUpper);
    }
  }

  private static void putUnsignedLongLong(Any any, long value, ORB orb) {
    any.type(orb.get_primitive_tc(TCKind.tk_ulonglong));
    org.omg.CORBA.portable.OutputStream output = any.create_output_stream();
    output.write_ulonglong(value);
    any.read_value(output.create_input_stream(), any.type());
  }

  private static void putUnsignedLong(Any any, long value, ORB orb) {
    any.type(orb.get_primitive_tc(TCKind.tk_ulong));
    org.omg.CORBA.portable.OutputStream output = any.create_output_stream();
    output.write_ulong((int) value);
    any.read_value(output.create_input_stream(), any.type());
  }

  private static void putUnsignedShort(Any any, int value, ORB orb) {
    any.type(orb.get_primitive_tc(TCKind.tk_ushort));
    org.omg.CORBA.portable.OutputStream output = any.create_output_stream();
    output.write_ushort((short) value);
    any.read_value(output.create_input_stream(), any.type());
  }

  private static TypeCode utcType(ORB orb) {
    return orb.create_struct_tc(
        UTC_TIME_REPOSITORY_ID,
        "UtcT",
        new StructMember[] {
          new StructMember("time", orb.get_primitive_tc(TCKind.tk_ulonglong), null),
          new StructMember("inacclo", orb.get_primitive_tc(TCKind.tk_ulong), null),
          new StructMember("inacchi", orb.get_primitive_tc(TCKind.tk_ushort), null),
          new StructMember("tdf", orb.get_primitive_tc(TCKind.tk_short), null)
        });
  }

  private static TypeCode intervalType(ORB orb) {
    return orb.create_struct_tc(
        INTERVAL_REPOSITORY_ID,
        "IntervalT",
        new StructMember[] {
          new StructMember("lower_bound", orb.get_primitive_tc(TCKind.tk_ulonglong), null),
          new StructMember("upper_bound", orb.get_primitive_tc(TCKind.tk_ulonglong), null)
        });
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

  private static final class TimeServiceServant extends DynamicImplementation {
    private final ORB orb;

    private TimeServiceServant(ORB orb) {
      this.orb = orb;
    }

    @Override
    public void invoke(ServerRequest request) {
      switch (request.operation()) {
        case "universal_time" ->
            request.set_result(
                utcAny(
                    TIME_SERVICE_CURRENT_TICKS,
                    TIME_SERVICE_CURRENT_INACCLO,
                    TIME_SERVICE_CURRENT_INACCHI,
                    TIME_SERVICE_CURRENT_TDF));
        case "new_universal_time" -> newUniversalTime(request);
        case "new_interval" -> newInterval(request);
        case "_is_a" -> isA(request);
        case "_non_existent" -> nonExistent(request);
        default -> throw new org.omg.CORBA.BAD_OPERATION(request.operation());
      }
    }

    private void newUniversalTime(ServerRequest request) {
      NVList arguments = orb.create_list(0);
      Any time = typedAny(orb.get_primitive_tc(TCKind.tk_ulonglong));
      Any inacclo = typedAny(orb.get_primitive_tc(TCKind.tk_ulong));
      Any inacchi = typedAny(orb.get_primitive_tc(TCKind.tk_ushort));
      Any tdf = typedAny(orb.get_primitive_tc(TCKind.tk_short));
      arguments.add_value("time", time, ARG_IN.value);
      arguments.add_value("inacclo", inacclo, ARG_IN.value);
      arguments.add_value("inacchi", inacchi, ARG_IN.value);
      arguments.add_value("tdf", tdf, ARG_IN.value);
      request.arguments(arguments);
      org.omg.CORBA.portable.InputStream timeIn = time.create_input_stream();
      org.omg.CORBA.portable.InputStream inaccloIn = inacclo.create_input_stream();
      org.omg.CORBA.portable.InputStream inacchiIn = inacchi.create_input_stream();
      request.set_result(
          utcAny(
              timeIn.read_ulonglong(),
              inaccloIn.read_ulong(),
              inacchiIn.read_ushort(),
              tdf.extract_short()));
    }

    private void newInterval(ServerRequest request) {
      NVList arguments = orb.create_list(0);
      Any lower = typedAny(orb.get_primitive_tc(TCKind.tk_ulonglong));
      Any upper = typedAny(orb.get_primitive_tc(TCKind.tk_ulonglong));
      arguments.add_value("lower_bound", lower, ARG_IN.value);
      arguments.add_value("upper_bound", upper, ARG_IN.value);
      request.arguments(arguments);
      org.omg.CORBA.portable.InputStream lowerIn = lower.create_input_stream();
      org.omg.CORBA.portable.InputStream upperIn = upper.create_input_stream();
      Any result = orb.create_any();
      result.type(intervalType(orb));
      org.omg.CORBA.portable.OutputStream output = result.create_output_stream();
      output.write_ulonglong(lowerIn.read_ulonglong());
      output.write_ulonglong(upperIn.read_ulonglong());
      result.read_value(output.create_input_stream(), result.type());
      request.set_result(result);
    }

    private Any utcAny(long time, long inacclo, int inacchi, short tdf) {
      Any result = orb.create_any();
      result.type(utcType(orb));
      org.omg.CORBA.portable.OutputStream output = result.create_output_stream();
      output.write_ulonglong(time);
      output.write_ulong((int) inacclo);
      output.write_ushort((short) inacchi);
      output.write_short(tdf);
      result.read_value(output.create_input_stream(), result.type());
      return result;
    }

    private Any typedAny(TypeCode type) {
      Any value = orb.create_any();
      value.type(type);
      return value;
    }

    private void isA(ServerRequest request) {
      NVList arguments = orb.create_list(0);
      Any repositoryId = orb.create_any();
      repositoryId.type(orb.get_primitive_tc(TCKind.tk_string));
      arguments.add_value("repository_id", repositoryId, ARG_IN.value);
      request.arguments(arguments);
      Any result = orb.create_any();
      result.insert_boolean(TIME_SERVICE_REPOSITORY_ID.equals(repositoryId.extract_string()));
      request.set_result(result);
    }

    private void nonExistent(ServerRequest request) {
      Any result = orb.create_any();
      result.insert_boolean(false);
      request.set_result(result);
    }

    @Override
    public String[] _all_interfaces(POA poa, byte[] objectId) {
      return new String[] {TIME_SERVICE_REPOSITORY_ID};
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
        org.omg.CORBA.portable.OutputStream out = problem.create_output_stream();
        out.write_string(PROBLEM_REPOSITORY_ID);
        problem.read_value(out.create_input_stream(), problemType());
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
