package io.github.mundanej.mjo.nativeimage;

import java.util.List;

/** G6 Native Image smoke target catalog. */
public final class NativeImageTargets {

  private static final String PACKAGE = "io.github.mundanej.mjo.nativeimage.smoke.";
  private static final NativeImageTarget IDLJ_VALIDATE =
      target("idljValidate", "IdljValidateNativeSmoke", "idlj-validate-native-smoke");
  private static final NativeImageTarget GENERATED_CLIENT =
      target("generatedClient", "GeneratedClientNativeSmoke", "generated-client-native-smoke");
  private static final NativeImageTarget GENERATED_SERVER =
      target("generatedServer", "GeneratedServerNativeSmoke", "generated-server-native-smoke");
  private static final NativeImageTarget NAMING_SERVER =
      target("namingServer", "NamingServerNativeSmoke", "naming-server-native-smoke");
  private static final NativeImageTarget IOR_DIAGNOSTICS =
      target("iorDiagnostics", "IorDiagnosticsNativeSmoke", "ior-diagnostics-native-smoke");
  private static final NativeImageTarget INTEROP_REPORT =
      target("interopReport", "InteropReportNativeSmoke", "interop-report-native-smoke");
  private static final NativeImageTarget RMI_IIOP =
      target("rmiIiop", "RmiIiopNativeSmoke", "rmi-iiop-native-smoke");
  private static final NativeImageTarget INTEROP_CLIENT =
      target("interopClient", "InteropClientNativeSmoke", "interop-client-native-smoke");
  private static final NativeImageTarget INTEROP_SERVER =
      target("interopServer", "InteropServerNativeSmoke", "interop-server-native-smoke");
  private static final List<NativeImageTarget> G6_TARGETS =
      List.of(
          IDLJ_VALIDATE,
          GENERATED_CLIENT,
          GENERATED_SERVER,
          NAMING_SERVER,
          IOR_DIAGNOSTICS,
          INTEROP_REPORT);
  private static final List<NativeImageTarget> G10_TARGETS =
      List.of(
          IDLJ_VALIDATE,
          GENERATED_CLIENT,
          GENERATED_SERVER,
          NAMING_SERVER,
          IOR_DIAGNOSTICS,
          INTEROP_REPORT,
          RMI_IIOP,
          INTEROP_CLIENT,
          INTEROP_SERVER);

  private NativeImageTargets() {}

  /** Returns the G6-910 smoke binaries in deterministic execution order. */
  public static List<NativeImageTarget> g6Targets() {
    return G6_TARGETS;
  }

  /** Returns the G10-100 interop smoke binaries in deterministic execution order. */
  public static List<NativeImageTarget> g10Targets() {
    return G10_TARGETS;
  }

  private static NativeImageTarget target(String name, String mainClassName, String binaryName) {
    return new NativeImageTarget(name, PACKAGE + mainClassName, binaryName);
  }
}
