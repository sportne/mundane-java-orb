package io.github.mundanej.mjo.nativeimage;

import io.github.mundanej.mjo.nativeimage.smoke.EventServiceNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.GeneratedClientNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.GeneratedServerNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.IdljValidateNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.InteropClientNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.InteropReportNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.InteropServerNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.IorDiagnosticsNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.LiveInteropLane;
import io.github.mundanej.mjo.nativeimage.smoke.NamingServerNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.NotificationServiceNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.RmiIiopNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.SecurityServiceNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.TimeServiceNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.TradingServiceNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.TransactionServiceNativeSmoke;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeImageSmokeEntrypointTest {

  @TempDir Path temporaryDirectory;

  @Test
  void smokeEntrypointsRunOnJvmBeforeNativeCompilation() throws Exception {
    IdljValidateNativeSmoke.main(new String[0]);
    GeneratedClientNativeSmoke.main(new String[0]);
    GeneratedServerNativeSmoke.main(new String[0]);
    NamingServerNativeSmoke.main(new String[0]);
    IorDiagnosticsNativeSmoke.main(new String[0]);
    InteropReportNativeSmoke.main(new String[0]);
    RmiIiopNativeSmoke.main(new String[0]);
    TimeServiceNativeSmoke.main(new String[0]);
    EventServiceNativeSmoke.main(new String[0]);
    NotificationServiceNativeSmoke.main(new String[0]);
    TradingServiceNativeSmoke.main(new String[0]);
    TransactionServiceNativeSmoke.main(new String[0]);
    SecurityServiceNativeSmoke.main(new String[0]);
    InteropClientNativeSmoke.main(new String[0]);
    InteropServerNativeSmoke.main(new String[0]);
  }

  @Test
  void representativeSmokeEntrypointsRemainDeterministicAcrossBoundedRuns() throws Exception {
    for (int iteration = 0; iteration < 16; iteration++) {
      IdljValidateNativeSmoke.main(new String[0]);
      IorDiagnosticsNativeSmoke.main(new String[0]);
      InteropReportNativeSmoke.main(new String[0]);
      RmiIiopNativeSmoke.main(new String[0]);
      InteropClientNativeSmoke.main(new String[0]);
      InteropServerNativeSmoke.main(new String[0]);
    }
  }

  @Test
  void liveLivenessLaneRunsOnJvmAgainstLocalServer() throws Exception {
    Path ior = temporaryDirectory.resolve("basic-idl-server.ior");
    Map<String, String> env =
        Map.of(
            "MJO_INTEROP_SCENARIO",
            "basic-idl",
            "MJO_INTEROP_SERVER_IOR",
            ior.toString(),
            "MJO_INTEROP_BIND_HOST",
            "127.0.0.1",
            "MJO_INTEROP_ADVERTISE_HOST",
            "127.0.0.1");

    LiveInteropLane.RunningServer server = LiveInteropLane.startServer(env);
    try {
      LiveInteropLane.runClient(env);
    } finally {
      server.close();
    }
  }

  @Test
  void liveLivenessLaneAcceptsExistingJavaPeerSmokeRepositoryId() throws Exception {
    Path ior = temporaryDirectory.resolve("basic-idl-server.ior");
    Map<String, String> env =
        Map.of(
            "MJO_INTEROP_SCENARIO",
            "basic-idl",
            "MJO_INTEROP_SERVER_IOR",
            ior.toString(),
            "MJO_INTEROP_BIND_HOST",
            "127.0.0.1",
            "MJO_INTEROP_ADVERTISE_HOST",
            "127.0.0.1");

    LiveInteropLane.RunningServer server = LiveInteropLane.startServer(env);
    try {
      LiveInteropLane.assertLegacySmokeRepositoryId(env);
    } finally {
      server.close();
    }
  }

  @Test
  void liveRmiIiopCalculatorLaneRunsOnJvmAgainstLocalServer() throws Exception {
    Path ior = temporaryDirectory.resolve("rmi-iiop-server.ior");
    Map<String, String> env =
        Map.of(
            "MJO_INTEROP_SCENARIO",
            "rmi-iiop",
            "MJO_INTEROP_SERVER_IOR",
            ior.toString(),
            "MJO_INTEROP_BIND_HOST",
            "127.0.0.1",
            "MJO_INTEROP_ADVERTISE_HOST",
            "127.0.0.1");

    LiveInteropLane.RunningServer server = LiveInteropLane.startServer(env);
    try {
      LiveInteropLane.runClient(env);
    } finally {
      server.close();
    }
  }

  @Test
  void liveTimeServiceLaneRunsOnJvmAgainstLocalServer() throws Exception {
    Path ior = temporaryDirectory.resolve("time-service-server.ior");
    Map<String, String> env =
        Map.of(
            "MJO_INTEROP_SCENARIO",
            "time-service",
            "MJO_INTEROP_SERVER_IOR",
            ior.toString(),
            "MJO_INTEROP_BIND_HOST",
            "127.0.0.1",
            "MJO_INTEROP_ADVERTISE_HOST",
            "127.0.0.1");

    LiveInteropLane.RunningServer server = LiveInteropLane.startServer(env);
    try {
      LiveInteropLane.runClient(env);
    } finally {
      server.close();
    }
  }
}
