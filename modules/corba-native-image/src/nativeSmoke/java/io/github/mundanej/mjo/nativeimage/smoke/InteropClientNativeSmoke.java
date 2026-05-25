package io.github.mundanej.mjo.nativeimage.smoke;

/** Native Image smoke entry point for our pre-1.0 interop client lanes. */
public final class InteropClientNativeSmoke {

  private InteropClientNativeSmoke() {}

  /** Runs deterministic client-side interop smoke paths on the JVM or as a native binary. */
  public static void main(String[] args) throws Exception {
    if (LiveInteropLane.isEnabled(System.getenv())) {
      LiveInteropLane.runClient(System.getenv());
      return;
    }
    IdljValidateNativeSmoke.main(new String[0]);
    GeneratedClientNativeSmoke.main(new String[0]);
    IorDiagnosticsNativeSmoke.main(new String[0]);
    NamingServerNativeSmoke.main(new String[0]);
    InteropReportNativeSmoke.main(new String[0]);
    RmiIiopNativeSmoke.main(new String[0]);
  }
}
