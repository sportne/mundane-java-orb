package io.github.mundanej.mjo.interop.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

final class InteropReportTest {
  @Test
  void reportRoundTripsThroughDeterministicJson() {
    InteropReport report = sampleReport("notes with newline\nand quote \"value\"");

    String json = report.toJson();
    InteropReport parsed = InteropReport.fromJson(json);

    assertEquals(report, parsed);
    assertTrue(json.indexOf("\"peer\"") < json.indexOf("\"peerVersion\""));
    assertTrue(json.contains("\"classification\": \"infrastructure-failure\""));
  }

  @Test
  void rejectsMissingRequiredReportFields() {
    InteropReport report = sampleReport("");
    String missingPeer = report.toJson().replace("  \"peer\": \"jacorb\",\n", "");

    IllegalArgumentException failure =
        assertThrows(IllegalArgumentException.class, () -> InteropReport.fromJson(missingPeer));

    assertEquals("peer is required", failure.getMessage());
  }

  @Test
  void rejectsMalformedReportJsonDeterministically() {
    assertEquals(
        "json must not be blank",
        assertThrows(IllegalArgumentException.class, () -> InteropReport.fromJson(" "))
            .getMessage());
    assertEquals(
        "json object expected",
        assertThrows(IllegalArgumentException.class, () -> InteropReport.fromJson("[]"))
            .getMessage());
    assertEquals(
        "unsupported json escape: b",
        assertThrows(
                IllegalArgumentException.class,
                () -> InteropReport.fromJson("{\"peer\": \"bad\\b\"}"))
            .getMessage());
    assertEquals(
        "unterminated json string",
        assertThrows(
                IllegalArgumentException.class, () -> InteropReport.fromJson("{\"peer\": \"x}"))
            .getMessage());
  }

  @Test
  void normalizesNullNotesButRejectsBlankRequiredReportFields() {
    InteropReport report =
        new InteropReport(
            "jacorb",
            "3.9",
            "basic-idl",
            "interop/idl/basic/BasicTypes.idl",
            "our-jvm-jdk21",
            "peer-jvm-openjdk21",
            InteropRole.CLIENT,
            "corba-interop-peer-jacorb:3.9",
            "client",
            InteropReportStatus.PASSED,
            InteropFailureClassification.EXPECTED_DEFERRAL,
            0,
            "stdout.log",
            "stderr.log",
            "report.json",
            "2026-05-18T00:00:00Z",
            "2026-05-18T00:00:01Z",
            null);

    assertEquals("", report.notes());
    assertEquals(
        "peer must not be blank",
        assertThrows(
                IllegalArgumentException.class,
                () ->
                    new InteropReport(
                        " ",
                        "3.9",
                        "basic-idl",
                        "interop/idl/basic/BasicTypes.idl",
                        "our-jvm-jdk21",
                        "peer-jvm-openjdk21",
                        InteropRole.CLIENT,
                        "corba-interop-peer-jacorb:3.9",
                        "client",
                        InteropReportStatus.PASSED,
                        InteropFailureClassification.EXPECTED_DEFERRAL,
                        0,
                        "stdout.log",
                        "stderr.log",
                        "report.json",
                        "2026-05-18T00:00:00Z",
                        "2026-05-18T00:00:01Z",
                        ""))
            .getMessage());
  }

  @Test
  void rejectsUnknownWireValues() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InteropFailureClassification.fromWireName("unreviewed"));
    assertThrows(IllegalArgumentException.class, () -> InteropReportStatus.fromWireName("unknown"));
    assertThrows(IllegalArgumentException.class, () -> InteropRole.fromWireName("operator"));
  }

  @Test
  void commandResultRequiresOrderedTimestamps() {
    Instant now = Instant.parse("2026-05-18T00:00:00Z");

    assertThrows(
        IllegalArgumentException.class,
        () -> new InteropCommandResult(0, "stdout.log", "stderr.log", now, now.minusSeconds(1)));
  }

  @Test
  void orchestrationValueTypesValidateRequiredFields() {
    InteropRuntime client = new InteropRuntime("our-jvm-jdk21");
    InteropRuntime server = new InteropRuntime("peer-jvm");

    assertEquals(client, new InteropDirection(client, server).clientRuntime());
    assertEquals("jacorb", new InteropPeer("jacorb", "3.9").name());
    assertEquals("basic-idl", new InteropScenario("basic-idl", "interop/idl/basic").name());
    assertEquals(
        new InteropScenario("rmi-iiop", "interop/idl/rmi-iiop/Calculator.idl"),
        InteropScenario.rmiIiop());
    assertThrows(IllegalArgumentException.class, () -> new InteropRuntime(" "));
    assertThrows(NullPointerException.class, () -> new InteropDirection(null, server));
    assertThrows(IllegalArgumentException.class, () -> new InteropPeer("", "3.9"));
    assertThrows(IllegalArgumentException.class, () -> new InteropScenario("basic-idl", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InteropScenario("bad scenario", "interop/idl/basic/BasicTypes.idl"));
    assertThrows(
        IllegalArgumentException.class, () -> new InteropScenario("bad", "../BasicTypes.idl"));
  }

  private static InteropReport sampleReport(String notes) {
    return new InteropReport(
        "jacorb",
        "3.9",
        "basic-idl",
        "interop/idl/basic/BasicTypes.idl",
        "our-jvm-jdk21",
        "peer-jvm-openjdk21",
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
        notes);
  }
}
