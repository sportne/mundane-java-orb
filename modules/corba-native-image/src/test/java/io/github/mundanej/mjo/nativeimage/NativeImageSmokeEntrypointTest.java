package io.github.mundanej.mjo.nativeimage;

import io.github.mundanej.mjo.nativeimage.smoke.GeneratedClientNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.GeneratedServerNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.IdljValidateNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.InteropReportNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.IorDiagnosticsNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.NamingServerNativeSmoke;
import io.github.mundanej.mjo.nativeimage.smoke.RmiIiopNativeSmoke;
import org.junit.jupiter.api.Test;

final class NativeImageSmokeEntrypointTest {

  @Test
  void smokeEntrypointsRunOnJvmBeforeNativeCompilation() throws Exception {
    IdljValidateNativeSmoke.main(new String[0]);
    GeneratedClientNativeSmoke.main(new String[0]);
    GeneratedServerNativeSmoke.main(new String[0]);
    NamingServerNativeSmoke.main(new String[0]);
    IorDiagnosticsNativeSmoke.main(new String[0]);
    InteropReportNativeSmoke.main(new String[0]);
    RmiIiopNativeSmoke.main(new String[0]);
  }

  @Test
  void representativeSmokeEntrypointsRemainDeterministicAcrossBoundedRuns() throws Exception {
    for (int iteration = 0; iteration < 16; iteration++) {
      IdljValidateNativeSmoke.main(new String[0]);
      IorDiagnosticsNativeSmoke.main(new String[0]);
      InteropReportNativeSmoke.main(new String[0]);
      RmiIiopNativeSmoke.main(new String[0]);
    }
  }
}
