package io.github.mundanej.mjo.rmi.iiop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.common.DiagnosticCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for local RMI-IIOP CDR value and operation codecs. */
@Tag("unit")
final class RmiCdrOperationCodecTest {

  private static final RmiJavaTypeReference REMOTE_EXCEPTION =
      RmiJavaTypeReference.declared("java.rmi.RemoteException");
  private static final List<String> FORBIDDEN_RUNTIME_TOKENS =
      List.of(
          "Class.forName",
          "java.lang.reflect",
          "Proxy.newProxyInstance",
          "ObjectInputStream",
          "ObjectOutputStream",
          "java.io.Serializable",
          "ObjectStreamClass",
          "ServiceLoader",
          "ClassLoader");

  private final RmiCdrValueCodec valueCodec = new RmiCdrValueCodec();
  private final RmiCdrOperationCodec operationCodec =
      new RmiCdrOperationCodec(approvedRepositoryIdPlan());

  @Test
  void roundTripsPrimitiveAndStringValues() {
    CdrWriter writer = CdrWriter.bigEndian().writeOctet(0xCC);
    valueCodec.writeValue(
        writer, RmiIdlTypeReference.builtin("boolean"), RmiCdrValue.booleanValue(true));
    valueCodec.writeValue(
        writer, RmiIdlTypeReference.builtin("octet"), RmiCdrValue.octetValue((byte) 7));
    valueCodec.writeValue(
        writer,
        RmiIdlTypeReference.builtin("char"),
        new RmiCdrValue(RmiIdlTypeReference.builtin("char"), 'A'));
    valueCodec.writeValue(
        writer, RmiIdlTypeReference.builtin("wchar"), RmiCdrValue.wcharValue('\u03A9'));
    valueCodec.writeValue(
        writer, RmiIdlTypeReference.builtin("short"), RmiCdrValue.shortValue((short) 12));
    valueCodec.writeValue(writer, RmiIdlTypeReference.builtin("long"), RmiCdrValue.longValue(34));
    valueCodec.writeValue(
        writer, RmiIdlTypeReference.builtin("long long"), RmiCdrValue.longLongValue(56L));
    valueCodec.writeValue(
        writer, RmiIdlTypeReference.builtin("float"), RmiCdrValue.floatValue(1.5F));
    valueCodec.writeValue(
        writer, RmiIdlTypeReference.builtin("double"), RmiCdrValue.doubleValue(2.5D));
    valueCodec.writeValue(
        writer,
        RmiIdlTypeReference.builtin("string"),
        new RmiCdrValue(RmiIdlTypeReference.builtin("string"), "plain"));
    valueCodec.writeValue(
        writer, RmiIdlTypeReference.builtin("wstring"), RmiCdrValue.stringValue("A\u03A9"));

    CdrReader reader = CdrReader.bigEndian(writer.toByteArray());
    assertEquals(0xCC, reader.readOctet());
    assertEquals(
        RmiCdrValue.booleanValue(true),
        valueCodec.readValue(reader, RmiIdlTypeReference.builtin("boolean")));
    assertEquals(
        RmiCdrValue.octetValue((byte) 7),
        valueCodec.readValue(reader, RmiIdlTypeReference.builtin("octet")));
    assertEquals(
        new RmiCdrValue(RmiIdlTypeReference.builtin("char"), 'A'),
        valueCodec.readValue(reader, RmiIdlTypeReference.builtin("char")));
    assertEquals(
        RmiCdrValue.wcharValue('\u03A9'),
        valueCodec.readValue(reader, RmiIdlTypeReference.builtin("wchar")));
    assertEquals(
        RmiCdrValue.shortValue((short) 12),
        valueCodec.readValue(reader, RmiIdlTypeReference.builtin("short")));
    assertEquals(
        RmiCdrValue.longValue(34),
        valueCodec.readValue(reader, RmiIdlTypeReference.builtin("long")));
    assertEquals(
        RmiCdrValue.longLongValue(56L),
        valueCodec.readValue(reader, RmiIdlTypeReference.builtin("long long")));
    assertEquals(
        RmiCdrValue.floatValue(1.5F),
        valueCodec.readValue(reader, RmiIdlTypeReference.builtin("float")));
    assertEquals(
        RmiCdrValue.doubleValue(2.5D),
        valueCodec.readValue(reader, RmiIdlTypeReference.builtin("double")));
    assertEquals(
        new RmiCdrValue(RmiIdlTypeReference.builtin("string"), "plain"),
        valueCodec.readValue(reader, RmiIdlTypeReference.builtin("string")));
    assertEquals(
        RmiCdrValue.stringValue("A\u03A9"),
        valueCodec.readValue(reader, RmiIdlTypeReference.builtin("wstring")));
    assertEquals(0, reader.remaining());
  }

  @Test
  void wideStringsUsePeerUtf16OctetLengthWithBomAndAcceptPeerPayloadsWithoutBom() {
    CdrWriter writer = CdrWriter.bigEndian();

    valueCodec.writeValue(
        writer, RmiIdlTypeReference.builtin("wstring"), RmiCdrValue.stringValue("A\u03A9"));

    assertArrayEquals(
        new byte[] {0, 0, 0, 6, (byte) 0xFE, (byte) 0xFF, 0, 0x41, 0x03, (byte) 0xA9},
        writer.toByteArray());
    assertEquals(
        RmiCdrValue.stringValue("A\u03A9"),
        valueCodec.readValue(
            CdrReader.bigEndian(writer.toByteArray()), RmiIdlTypeReference.builtin("wstring")));

    CdrWriter peerPayload = CdrWriter.bigEndian();
    peerPayload.writeUnsignedLong(4).writeUnsignedShort('A').writeUnsignedShort(0x03A9);
    assertEquals(
        RmiCdrValue.stringValue("A\u03A9"),
        valueCodec.readValue(
            CdrReader.bigEndian(peerPayload.toByteArray()),
            RmiIdlTypeReference.builtin("wstring")));

    CdrWriter swappedBomPayload = CdrWriter.bigEndian();
    swappedBomPayload.writeUnsignedLong(2).writeUnsignedShort(0xFFFE);
    assertThrows(
        io.github.mundanej.mjo.cdr.CdrException.class,
        () ->
            valueCodec.readValue(
                CdrReader.bigEndian(swappedBomPayload.toByteArray()),
                RmiIdlTypeReference.builtin("wstring")));
  }

  @Test
  void roundTripsOperationArgumentsReturnValuesAndVoidMarkers() {
    RmiIdlOperation add = operation("add");
    RmiIdlOperation clear = operation("clear");
    CdrWriter argumentWriter = CdrWriter.littleEndian();
    operationCodec.writeArguments(
        argumentWriter, add, List.of(RmiCdrValue.longValue(13), RmiCdrValue.longValue(29)));

    List<RmiCdrValue> arguments =
        operationCodec.readArguments(CdrReader.littleEndian(argumentWriter.toByteArray()), add);

    assertEquals(List.of(RmiCdrValue.longValue(13), RmiCdrValue.longValue(29)), arguments);

    CdrWriter returnWriter = CdrWriter.bigEndian();
    operationCodec.writeReturnValue(returnWriter, add, RmiCdrValue.longValue(42));
    assertEquals(
        RmiCdrValue.longValue(42),
        operationCodec.readReturnValue(CdrReader.bigEndian(returnWriter.toByteArray()), add));

    CdrWriter voidWriter = CdrWriter.bigEndian();
    operationCodec.writeReturnValue(voidWriter, clear, RmiCdrValue.voidValue());
    assertEquals(0, voidWriter.toByteArray().length);
    assertEquals(
        RmiCdrValue.voidValue(),
        operationCodec.readReturnValue(CdrReader.bigEndian(voidWriter.toByteArray()), clear));
  }

  @Test
  void roundTripsSequencesRemoteObjectsDeclaredValuesAndExceptionPayloads() {
    RmiIdlTypeReference longType = RmiIdlTypeReference.builtin("long");
    RmiIdlTypeReference sequenceType = RmiIdlTypeReference.sequenceOf(longType);
    RmiIdlTypeReference remoteType =
        RmiIdlTypeReference.remoteObject("::example::calc::Calculator", "example.calc.Calculator");
    RmiIdlTypeReference valueType =
        RmiIdlTypeReference.declaredValue(
            "::example::calc::Reading",
            "example.calc.Reading",
            List.of(
                new RmiIdlValueMember("label", RmiIdlTypeReference.builtin("wstring")),
                new RmiIdlValueMember("count", longType)));
    RmiCdrValue sequence =
        RmiCdrValue.sequenceValue(
            longType, List.of(RmiCdrValue.longValue(1), RmiCdrValue.longValue(2)));
    RmiCdrValue objectReference =
        RmiCdrValue.objectReferenceValue(remoteType, RmiIiopObjectKey.fromString("remote-1"));
    RmiCdrDeclaredValue declaredPayload =
        new RmiCdrDeclaredValue(
            "RMI:example.calc.Reading:3333333333333333",
            List.of(RmiCdrValue.stringValue("today"), RmiCdrValue.longValue(7)));
    RmiCdrValue declaredValue = RmiCdrValue.declaredValue(valueType, declaredPayload);
    CdrWriter writer = CdrWriter.bigEndian();

    valueCodec.writeValue(writer, sequenceType, sequence);
    valueCodec.writeValue(writer, remoteType, objectReference);
    valueCodec.writeValue(writer, valueType, declaredValue);
    CdrReader reader = CdrReader.bigEndian(writer.toByteArray());

    assertEquals(sequence, valueCodec.readValue(reader, sequenceType));
    assertEquals(objectReference, valueCodec.readValue(reader, remoteType));
    assertEquals(declaredValue, valueCodec.readValue(reader, valueType));
    assertEquals(0, reader.remaining());

    RmiIdlExceptionReference problem =
        new RmiIdlExceptionReference(
            "example.calc.CalculatorProblem",
            "::example::calc::CalculatorProblem",
            List.of(new RmiIdlValueMember("reason", RmiIdlTypeReference.builtin("wstring"))));
    RmiIdlOperation operation =
        new RmiIdlOperation(
            "describe", RmiIdlTypeReference.voidType(), List.of(), List.of(problem));
    CdrWriter exceptionWriter = CdrWriter.bigEndian();

    operationCodec.writeUserException(
        exceptionWriter, operation, problem, List.of(RmiCdrValue.stringValue("bad")));
    RmiCdrUserExceptionPayload payload =
        operationCodec.readUserException(
            CdrReader.bigEndian(exceptionWriter.toByteArray()), operation);

    assertEquals(problem, payload.exception());
    assertEquals(List.of(RmiCdrValue.stringValue("bad")), payload.fields());
  }

  @Test
  void roundTripsEmptyDeclaredUserExceptionPayloadByRepositoryId() {
    RmiIdlOperation describe = operation("describe");
    RmiIdlExceptionReference problem = describe.exceptions().getFirst();
    CdrWriter writer = CdrWriter.bigEndian();

    operationCodec.writeUserException(writer, describe, problem);
    RmiCdrUserExceptionPayload payload =
        operationCodec.readUserException(CdrReader.bigEndian(writer.toByteArray()), describe);

    assertEquals(problem, payload.exception());
    assertEquals("RMI:example.calc.CalculatorProblem:2222222222222222", payload.repositoryId());
  }

  @Test
  void rejectsMissingAndUndeclaredExceptionRepositoryIds() {
    RmiIdlOperation describe = operation("describe");
    RmiIdlExceptionReference problem = describe.exceptions().getFirst();
    RmiCdrOperationCodec missingRepositoryId =
        new RmiCdrOperationCodec(
            new RmiRepositoryIdPlan(
                List.of(
                    new RmiRepositoryIdValue(
                        "example.calc.Calculator",
                        "RMI:example.calc.Calculator:0123456789ABCDEF"))));

    assertRmiCode(
        RmiJavaDiagnosticCodes.CDR_MISSING_EXCEPTION_REPOSITORY_ID,
        () -> missingRepositoryId.writeUserException(CdrWriter.bigEndian(), describe, problem));

    CdrWriter undeclared = CdrWriter.bigEndian();
    undeclared.writeString("RMI:example.calc.OtherProblem:3333333333333333");
    assertRmiCode(
        RmiJavaDiagnosticCodes.CDR_UNDECLARED_EXCEPTION_REPOSITORY_ID,
        () ->
            operationCodec.readUserException(
                CdrReader.bigEndian(undeclared.toByteArray()), describe));

    RmiIdlExceptionReference otherProblem =
        new RmiIdlExceptionReference("example.calc.OtherProblem", "::example::calc::OtherProblem");
    assertRmiCode(
        RmiJavaDiagnosticCodes.CDR_UNDECLARED_EXCEPTION_REPOSITORY_ID,
        () -> operationCodec.writeUserException(CdrWriter.bigEndian(), describe, otherProblem));
  }

  @Test
  void rejectsNullWrongKindWrongCountAndUnsupportedTypes() {
    RmiIdlOperation add = operation("add");

    assertRmiCode(
        RmiJavaDiagnosticCodes.CDR_NULL_VALUE,
        () ->
            valueCodec.writeValue(
                CdrWriter.bigEndian(),
                RmiIdlTypeReference.builtin("long"),
                new RmiCdrValue(RmiIdlTypeReference.builtin("long"), null)));
    assertRmiCode(
        RmiJavaDiagnosticCodes.CDR_VALUE_TYPE_MISMATCH,
        () ->
            valueCodec.writeValue(
                CdrWriter.bigEndian(),
                RmiIdlTypeReference.builtin("long"),
                new RmiCdrValue(RmiIdlTypeReference.builtin("long"), "not an int")));
    assertRmiCode(
        RmiJavaDiagnosticCodes.CDR_OPERATION_ARGUMENT_COUNT_MISMATCH,
        () ->
            operationCodec.writeArguments(
                CdrWriter.bigEndian(), add, List.of(RmiCdrValue.longValue(1))));
    assertRmiCode(
        RmiJavaDiagnosticCodes.CDR_VALUE_TYPE_MISMATCH,
        () ->
            valueCodec.writeValue(
                CdrWriter.bigEndian(),
                RmiIdlTypeReference.voidType(),
                new RmiCdrValue(RmiIdlTypeReference.voidType(), "not void")));
    assertRmiCode(
        RmiJavaDiagnosticCodes.CDR_VALUE_TYPE_MISMATCH,
        () ->
            valueCodec.writeValue(
                CdrWriter.bigEndian(),
                RmiIdlTypeReference.builtin("long"),
                RmiCdrValue.shortValue((short) 1)));
    assertRmiCode(
        RmiJavaDiagnosticCodes.CDR_VALUE_TYPE_MISMATCH,
        () ->
            valueCodec.writeValue(
                CdrWriter.bigEndian(),
                RmiIdlTypeReference.remoteObject("::example::Remote", "example.Remote"),
                new RmiCdrValue(
                    RmiIdlTypeReference.remoteObject("::example::Remote", "example.Remote"),
                    "not an object key")));
  }

  @Test
  void keepsRmiCdrDiagnosticCodeValuesStable() {
    assertEquals(
        List.of("RMI-0600", "RMI-0601", "RMI-0602", "RMI-0603", "RMI-0604", "RMI-0605"),
        List.of(
                RmiJavaDiagnosticCodes.UNSUPPORTED_CDR_MARSHALING_TYPE,
                RmiJavaDiagnosticCodes.CDR_VALUE_TYPE_MISMATCH,
                RmiJavaDiagnosticCodes.CDR_NULL_VALUE,
                RmiJavaDiagnosticCodes.CDR_OPERATION_ARGUMENT_COUNT_MISMATCH,
                RmiJavaDiagnosticCodes.CDR_MISSING_EXCEPTION_REPOSITORY_ID,
                RmiJavaDiagnosticCodes.CDR_UNDECLARED_EXCEPTION_REPOSITORY_ID)
            .stream()
            .map(DiagnosticCode::value)
            .toList());
  }

  @Test
  void mainSourcesAvoidForbiddenRuntimeMechanisms() throws Exception {
    Path sourceRoot = Path.of("src/main/java");
    try (Stream<Path> paths = Files.walk(sourceRoot)) {
      String sources =
          paths
              .filter(path -> path.toString().endsWith(".java"))
              .map(RmiCdrOperationCodecTest::readString)
              .reduce("", String::concat);

      assertEquals(
          List.of(),
          FORBIDDEN_RUNTIME_TOKENS.stream().filter(sources::contains).toList(),
          "RMI-IIOP main sources contain forbidden runtime mechanisms");
    }
  }

  private RmiIdlOperation operation(String name) {
    RmiIdlTranslationUnit translationUnit = approvedTranslationUnit();
    return interfaces(translationUnit).stream()
        .flatMap(idlInterface -> idlInterface.operations().stream())
        .filter(operation -> operation.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private static List<RmiIdlInterface> interfaces(RmiIdlTranslationUnit translationUnit) {
    return Stream.concat(
            translationUnit.interfaces().stream(),
            translationUnit.modules().stream().flatMap(RmiCdrOperationCodecTest::interfaces))
        .toList();
  }

  private static Stream<RmiIdlInterface> interfaces(RmiIdlModule module) {
    return Stream.concat(
        module.interfaces().stream(),
        module.modules().stream().flatMap(RmiCdrOperationCodecTest::interfaces));
  }

  private RmiIdlTranslationUnit approvedTranslationUnit() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.calc.Calculator",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "add",
                    RmiJavaTypeReference.primitive("int"),
                    List.of(
                        new RmiJavaParameter("left", RmiJavaTypeReference.primitive("int")),
                        new RmiJavaParameter("right", RmiJavaTypeReference.primitive("int"))),
                    List.of(REMOTE_EXCEPTION)),
                RmiJavaOperation.abstractOperation(
                    "describe",
                    RmiJavaTypeReference.declared("java.lang.String"),
                    List.of(
                        new RmiJavaParameter(
                            "name", RmiJavaTypeReference.declared("java.lang.String"))),
                    List.of(
                        RmiJavaTypeReference.declared("example.calc.CalculatorProblem"),
                        REMOTE_EXCEPTION)),
                RmiJavaOperation.abstractOperation(
                    "clear",
                    RmiJavaTypeReference.voidType(),
                    List.of(),
                    List.of(REMOTE_EXCEPTION))));

    RmiJavaToIdlResult result = new RmiJavaToIdlMapper().map(declaration);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    return result.translationUnit().orElseThrow();
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
    RmiCdrMarshalingException exception =
        assertThrows(RmiCdrMarshalingException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  private static String readString(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to read " + path, exception);
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run();
  }
}
