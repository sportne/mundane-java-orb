package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.interop.testkit.InteropFailureClassification;
import io.github.mundanej.mjo.interop.testkit.InteropReport;
import io.github.mundanej.mjo.interop.testkit.InteropReportStatus;
import io.github.mundanej.mjo.interop.testkit.InteropRole;

/** Native Image smoke entry point for structured interop report handling. */
public final class InteropReportNativeSmoke {

  private InteropReportNativeSmoke() {}

  /** Serializes and parses a clean-room interop report. */
  public static void main(String[] args) {
    InteropReport report =
        new InteropReport(
            "jacorb",
            "3.9",
            "basic-idl",
            "interop/idl/basic/BasicTypes.idl",
            "our-jvm-jdk21",
            "peer-jvm",
            InteropRole.SERVER,
            "corba-interop-peer-jacorb:3.9",
            "server",
            InteropReportStatus.FAILED,
            InteropFailureClassification.INFRASTRUCTURE_FAILURE,
            125,
            "build/interop/jacorb/logs/basic-idl-server.stdout.log",
            "build/interop/jacorb/logs/basic-idl-server.stderr.log",
            "build/interop/jacorb/reports/basic-idl-server.json",
            "2026-05-18T00:00:00Z",
            "2026-05-18T00:00:01Z",
            "native image smoke");

    InteropReport parsed = InteropReport.fromJson(report.toJson());
    SmokeAssertions.requireEquals(report, parsed, "report round trip");
    SmokeAssertions.require(
        parsed.toJson().contains("\"classification\": \"infrastructure-failure\""),
        "classification wire name");
  }
}
