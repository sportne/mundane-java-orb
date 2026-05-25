package io.github.mundanej.mjo.nativeimage.smoke;

/** Native Image smoke entry point for our pre-1.0 interop server lanes. */
public final class InteropServerNativeSmoke {

  private InteropServerNativeSmoke() {}

  /** Runs deterministic server-side interop smoke paths on the JVM or as a native binary. */
  public static void main(String[] args) throws Exception {
    if (LiveInteropLane.isEnabled(System.getenv())) {
      LiveInteropLane.runServer(System.getenv());
      return;
    }
    GeneratedServerNativeSmoke.main(new String[0]);
    NamingServerNativeSmoke.main(new String[0]);
    InteropReportNativeSmoke.main(new String[0]);
    RmiIiopNativeSmoke.main(new String[0]);
  }
}
