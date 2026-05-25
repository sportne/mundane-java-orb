import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ServerRequest;
import org.omg.PortableServer.DynamicImplementation;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public final class PeerSmoke {
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
    ORB orb = orb();
    POA root = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    root.the_POAManager().activate();
    org.omg.CORBA.Object ref = root.servant_to_reference(new SmokeServant());
    Path ior = iorPath("server");
    Files.createDirectories(ior.getParent());
    Files.writeString(ior, orb.object_to_string(ref) + System.lineSeparator());
    System.out.println("peer smoke server ready: " + ior);
    orb.run();
  }

  private static void client() throws Exception {
    requireScenarioIdl();
    ORB orb = orb();
    Path ior = iorPath("server");
    String value = Files.readString(ior).trim();
    org.omg.CORBA.Object ref = orb.string_to_object(value);
    boolean exists = !ref._non_existent();
    boolean isSmoke = ref._is_a("IDL:interop/Smoke:1.0");
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
    ORB orb = orb();
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

  private static ORB orb() {
    Properties properties = new Properties();
    String orbClass = System.getenv("INTEROP_ORB_CLASS");
    String singleton = System.getenv("INTEROP_ORB_SINGLETON_CLASS");
    if (orbClass != null && !orbClass.isBlank()) {
      properties.setProperty("org.omg.CORBA.ORBClass", orbClass);
    }
    if (singleton != null && !singleton.isBlank()) {
      properties.setProperty("org.omg.CORBA.ORBSingletonClass", singleton);
    }
    return ORB.init(new String[0], properties);
  }

  private static void requireScenarioIdl() throws IOException {
    Path idl = Path.of("/interop/scenario.idl");
    if (!Files.isRegularFile(idl) || Files.readString(idl).isBlank()) {
      throw new IllegalStateException("scenario IDL is missing or empty: " + idl);
    }
  }

  private static Path iorPath(String role) {
    String scenario = System.getenv().getOrDefault("INTEROP_SCENARIO", "manual");
    return Path.of("/interop/iors", scenario + "-" + role + ".ior");
  }

  private static final class SmokeServant extends DynamicImplementation {
    @Override
    public void invoke(ServerRequest request) {
      Any result = _orb().create_any();
      String operation = request.operation();
      if ("_is_a".equals(operation)) {
        result.insert_boolean(true);
      } else if ("_non_existent".equals(operation)) {
        result.insert_boolean(false);
      } else {
        result.insert_string("ok");
      }
      request.set_result(result);
    }

    @Override
    public String[] _all_interfaces(POA poa, byte[] objectId) {
      return new String[] {"IDL:interop/Smoke:1.0"};
    }
  }
}
