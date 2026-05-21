package io.github.mundanej.mjo.rmi.iiop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageReader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopMessageWriter;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.iiop.IiopClient;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopServer;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for the bounded RMI-IIOP wire bridge. */
@Tag("unit")
final class RmiIiopWireIntegrationTest {

  private final RmiIiopWireCodec codec = new RmiIiopWireCodec(approvedRepositoryIdPlan());

  @Test
  void objectKeysAreBoundedAndDefensivelyCopied() {
    byte[] raw = {1, 2, 3};
    RmiIiopObjectKey key = RmiIiopObjectKey.fromBytes(raw);

    raw[0] = 9;
    byte[] exposed = key.bytes();
    exposed[1] = 8;

    assertArrayEquals(new byte[] {1, 2, 3}, key.bytes());
    assertEquals(RmiIiopObjectKey.fromBytes(new byte[] {1, 2, 3}), key);
    assertNotEquals(key, RmiIiopObjectKey.fromString("other"));
    assertRmiCode(
        RmiJavaDiagnosticCodes.INVALID_WIRE_OBJECT_KEY, () -> RmiIiopObjectKey.fromString(" "));
    assertRmiCode(
        RmiJavaDiagnosticCodes.INVALID_WIRE_OBJECT_KEY,
        () -> RmiIiopObjectKey.fromBytes(new byte[RmiIiopObjectKey.MAX_OCTETS + 1]));
  }

  @Test
  void codecRoundTripsArgumentsReturnValuesAndUserExceptions() {
    RmiIdlOperation add = operation("add");
    RmiIdlOperation describe = operation("describe");
    byte[] argumentBody =
        codec.encodeArguments(add, List.of(RmiCdrValue.longValue(13), RmiCdrValue.longValue(29)));
    GiopRequest request =
        new GiopRequest(
            GiopHeader.forType(GiopMessageType.REQUEST),
            1,
            3,
            RmiIiopObjectKey.fromString("local-1").bytes(),
            "add",
            List.of(),
            argumentBody);

    assertEquals(
        List.of(RmiCdrValue.longValue(13), RmiCdrValue.longValue(29)),
        codec.decodeArguments(request, add));

    GiopReply returnReply =
        new GiopReply(
            GiopHeader.forType(GiopMessageType.REPLY),
            1,
            GiopReplyStatus.NO_EXCEPTION,
            List.of(),
            codec.encodeReturnValue(add, RmiCdrValue.longValue(42)));
    assertEquals(RmiCdrValue.longValue(42), codec.decodeReturnValue(returnReply, add));

    RmiIdlExceptionReference problem = describe.exceptions().getFirst();
    GiopReply userReply =
        new GiopReply(
            GiopHeader.forType(GiopMessageType.REPLY),
            2,
            GiopReplyStatus.USER_EXCEPTION,
            List.of(),
            codec.encodeUserException(describe, problem));
    RmiCdrUserExceptionPayload payload = codec.decodeUserException(userReply, describe);
    assertEquals(problem, payload.exception());
    assertEquals("RMI:example.calc.CalculatorProblem:2222222222222222", payload.repositoryId());
  }

  @Test
  void malformedBodiesAndSystemFailuresUseStableDiagnostics() {
    RmiIdlOperation add = operation("add");
    GiopRequest malformedRequest =
        new GiopRequest(
            GiopHeader.forType(GiopMessageType.REQUEST),
            3,
            3,
            RmiIiopObjectKey.fromString("local-1").bytes(),
            "add",
            List.of(),
            CdrWriter.bigEndian().writeLong(1).toByteArray());

    assertRmiCode(
        RmiJavaDiagnosticCodes.MALFORMED_WIRE_BODY,
        () -> codec.decodeArguments(malformedRequest, add));

    RmiIiopWireException failure =
        new RmiIiopWireException(RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OBJECT_KEY, "missing object");
    GiopReply systemReply =
        new GiopReply(
            GiopHeader.forType(GiopMessageType.REPLY),
            4,
            GiopReplyStatus.SYSTEM_EXCEPTION,
            List.of(),
            codec.encodeSystemFailure(failure));

    assertEquals(
        RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OBJECT_KEY,
        codec.decodeSystemFailure(systemReply).code());

    for (DiagnosticCode code :
        List.of(
            RmiJavaDiagnosticCodes.INVALID_WIRE_OBJECT_KEY,
            RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OPERATION,
            RmiJavaDiagnosticCodes.MALFORMED_WIRE_BODY,
            RmiJavaDiagnosticCodes.UNSUPPORTED_WIRE_REPLY_STATUS,
            RmiJavaDiagnosticCodes.REMOTE_SYSTEM_EXCEPTION_REPLY,
            RmiJavaDiagnosticCodes.UNDECLARED_WIRE_USER_EXCEPTION)) {
      GiopReply codedReply =
          new GiopReply(
              GiopHeader.forType(GiopMessageType.REPLY),
              5,
              GiopReplyStatus.SYSTEM_EXCEPTION,
              List.of(),
              codec.encodeSystemFailure(new RmiIiopWireException(code, code.value())));
      assertEquals(code, codec.decodeSystemFailure(codedReply).code());
    }
  }

  @Test
  void requestGoldenWireAndUnsupportedReplyStatusesAreDeterministic() {
    RmiIdlOperation add = operation("add");
    RmiIiopObjectKey objectKey = RmiIiopObjectKey.fromString("local-1");
    byte[] arguments =
        codec.encodeArguments(add, List.of(RmiCdrValue.longValue(1), RmiCdrValue.longValue(2)));
    GiopRequest request =
        new GiopRequest(
            GiopHeader.forType(GiopMessageType.REQUEST),
            9,
            3,
            objectKey.bytes(),
            "add",
            List.of(),
            arguments);

    byte[] encoded = new GiopMessageWriter().write(request);
    GiopRequest decoded = (GiopRequest) new GiopMessageReader().read(encoded);
    assertEquals(9, decoded.requestId());
    assertArrayEquals(objectKey.bytes(), decoded.objectKey());
    assertEquals("add", decoded.operation());
    assertArrayEquals(arguments, decoded.body());

    try (IiopServer server =
            IiopServer.bind(
                IiopEndpoint.loopback(0),
                IiopOptions.defaults(),
                ignored ->
                    new GiopReply(
                        GiopHeader.forType(GiopMessageType.REPLY),
                        ignored.requestId(),
                        GiopReplyStatus.LOCATION_FORWARD,
                        List.of(),
                        new byte[0]));
        IiopClient iiopClient = IiopClient.connect(server.endpoint(), IiopOptions.defaults());
        RmiIiopWireClient wireClient =
            new RmiIiopWireClient(iiopClient, approvedRepositoryIdPlan())) {
      assertRmiCode(
          RmiJavaDiagnosticCodes.UNSUPPORTED_WIRE_REPLY_STATUS,
          () ->
              wireClient.invoke(
                  objectKey, add, List.of(RmiCdrValue.longValue(1), RmiCdrValue.longValue(2))));
    }
  }

  @Test
  void keepsRmiIiopWireDiagnosticCodeValuesStable() {
    assertEquals(
        List.of("RMI-0800", "RMI-0801", "RMI-0802", "RMI-0803", "RMI-0804", "RMI-0805", "RMI-0806"),
        List.of(
                RmiJavaDiagnosticCodes.INVALID_WIRE_OBJECT_KEY,
                RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OBJECT_KEY,
                RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OPERATION,
                RmiJavaDiagnosticCodes.MALFORMED_WIRE_BODY,
                RmiJavaDiagnosticCodes.UNSUPPORTED_WIRE_REPLY_STATUS,
                RmiJavaDiagnosticCodes.REMOTE_SYSTEM_EXCEPTION_REPLY,
                RmiJavaDiagnosticCodes.UNDECLARED_WIRE_USER_EXCEPTION)
            .stream()
            .map(DiagnosticCode::value)
            .toList());
  }

  private RmiIdlOperation operation(String name) {
    return approvedInterface().operations().stream()
        .filter(operation -> operation.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private static RmiIdlInterface approvedInterface() {
    return new RmiIdlInterface(
        "Calculator",
        "::example::calc::Calculator",
        java.util.Optional.of("example.calc.Calculator"),
        List.of(
            new RmiIdlOperation(
                "add",
                RmiIdlTypeReference.builtin("long"),
                List.of(
                    new RmiIdlParameter("left", RmiIdlTypeReference.builtin("long")),
                    new RmiIdlParameter("right", RmiIdlTypeReference.builtin("long"))),
                List.of()),
            new RmiIdlOperation(
                "describe",
                RmiIdlTypeReference.builtin("wstring"),
                List.of(new RmiIdlParameter("name", RmiIdlTypeReference.builtin("wstring"))),
                List.of(
                    new RmiIdlExceptionReference(
                        "example.calc.CalculatorProblem", "::example::calc::CalculatorProblem")))));
  }

  private static RmiRepositoryIdPlan approvedRepositoryIdPlan() {
    return new RmiRepositoryIdPlan(
        List.of(
            new RmiRepositoryIdValue(
                "example.calc.Calculator", "RMI:example.calc.Calculator:0123456789ABCDEF"),
            new RmiRepositoryIdValue(
                "example.calc.CalculatorProblem",
                "RMI:example.calc.CalculatorProblem:2222222222222222")));
  }

  private static void assertRmiCode(DiagnosticCode expectedCode, ThrowingRunnable runnable) {
    RmiIiopWireException exception = assertThrows(RmiIiopWireException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run() throws Exception;
  }
}
