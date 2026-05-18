package io.github.mundanej.mjo.any;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlFieldDescriptor;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** CDR payload tests for local descriptor-backed Any values. */
@Tag("unit")
final class AnyCodecTest {

  @Test
  void primitiveAnyValuesRoundTripThroughCdrPayloadCodecs() {
    assertEquals(new AnyValue<>(IdlTypeCode.LONG, 42), roundTrip(AnyCodecs.longCodec(), 42));
    assertEquals(
        new AnyValue<>(IdlTypeCode.STRING, "hello"), roundTrip(AnyCodecs.stringCodec(), "hello"));
    assertEquals(
        new AnyValue<>(IdlTypeCode.BOOLEAN, true), roundTrip(AnyCodecs.booleanCodec(), true));
    assertEquals(new AnyValue<>(IdlTypeCode.DOUBLE, 3.5), roundTrip(AnyCodecs.doubleCodec(), 3.5));
  }

  @Test
  void longDoubleAnyRoundTripsSixteenOctetPayload() {
    byte[] payload = new byte[16];
    for (int index = 0; index < payload.length; index++) {
      payload[index] = (byte) (index + 1);
    }
    AnyValueCodec<byte[]> codec = AnyCodecs.longDoubleCodec();
    CdrWriter writer = CdrWriter.bigEndian();

    codec.writeAny(writer, new AnyValue<>(codec.typeCode(), payload));
    AnyValue<byte[]> result = codec.readAny(CdrReader.bigEndian(writer.toByteArray()));

    assertEquals(IdlTypeCode.LONG_DOUBLE, result.typeCode());
    assertArrayEquals(payload, result.value());
    assertThrows(
        IllegalArgumentException.class, () -> codec.write(CdrWriter.bigEndian(), new byte[15]));
  }

  @Test
  void enumAnyUsesCdrOrdinalAndLocalEnumeratorNames() {
    IdlTypeCode colorType = colorType();
    AnyValueCodec<String> codec = AnyCodecs.enumeration(colorType);

    assertEquals(new AnyValue<>(colorType, "GREEN"), roundTrip(codec, "GREEN"));
    assertThrows(AnyException.class, () -> codec.write(CdrWriter.bigEndian(), "BLUE"));

    CdrWriter writer = CdrWriter.bigEndian();
    writer.writeUnsignedLong(7);
    AnyException failure =
        assertThrows(
            AnyException.class, () -> codec.read(CdrReader.bigEndian(writer.toByteArray())));
    assertEquals(AnyDiagnosticCodes.INVALID_ENUM_VALUE, failure.code());
  }

  @Test
  void structAndExceptionAnyValuesRoundTripInTypeCodeMemberOrder() {
    AnyValueCodec<AnyAggregateValue> pointCodec = pointCodec();
    AnyAggregateValue point =
        new AnyAggregateValue(
            pointType(),
            Map.of(
                "y",
                new AnyValue<>(IdlTypeCode.LONG, 20),
                "x",
                new AnyValue<>(IdlTypeCode.LONG, 10)));
    AnyValueCodec<AnyAggregateValue> problemCodec = problemCodec();
    AnyAggregateValue problem =
        new AnyAggregateValue(
            problemType(), Map.of("message", new AnyValue<>(IdlTypeCode.STRING, "failed")));

    assertEquals(new AnyValue<>(pointType(), point), roundTrip(pointCodec, point));
    assertEquals(new AnyValue<>(problemType(), problem), roundTrip(problemCodec, problem));
  }

  @Test
  void sequenceAnyValuesRoundTripForPrimitiveAndAggregateElements() {
    IdlTypeCode longSequenceType =
        IdlTypeCode.sequenceOf(IdlTypeCode.LONG, "sequence<long>", "java.util.List");
    AnyValueCodec<List<Integer>> longSequence =
        AnyCodecs.sequence(longSequenceType, AnyCodecs.longCodec());
    IdlTypeCode pointSequenceType =
        IdlTypeCode.sequenceOf(pointType(), "sequence<demo::Point>", "java.util.List");
    AnyValueCodec<List<AnyAggregateValue>> pointSequence =
        AnyCodecs.sequence(pointSequenceType, pointCodec());
    AnyAggregateValue point =
        new AnyAggregateValue(
            pointType(),
            Map.of(
                "x",
                new AnyValue<>(IdlTypeCode.LONG, 1),
                "y",
                new AnyValue<>(IdlTypeCode.LONG, 2)));

    assertEquals(
        new AnyValue<>(longSequenceType, List.of(1, 2, 3)),
        roundTrip(longSequence, List.of(1, 2, 3)));
    assertEquals(
        new AnyValue<>(pointSequenceType, List.of(point)),
        roundTrip(pointSequence, List.of(point)));
    assertThrows(
        UnsupportedOperationException.class,
        () -> longSequence.read(CdrReader.bigEndian(sequenceBytes())).clear());
  }

  @Test
  void aggregateCodecsRejectMissingExtraAndMismatchedMembers() {
    AnyValueCodec<AnyAggregateValue> pointCodec = pointCodec();

    AnyException missing =
        assertThrows(
            AnyException.class,
            () ->
                pointCodec.write(
                    CdrWriter.bigEndian(),
                    new AnyAggregateValue(
                        pointType(), Map.of("x", new AnyValue<>(IdlTypeCode.LONG, 1)))));
    AnyException extra =
        assertThrows(
            AnyException.class,
            () ->
                pointCodec.write(
                    CdrWriter.bigEndian(),
                    new AnyAggregateValue(
                        pointType(),
                        Map.of(
                            "x",
                            new AnyValue<>(IdlTypeCode.LONG, 1),
                            "y",
                            new AnyValue<>(IdlTypeCode.LONG, 2),
                            "z",
                            new AnyValue<>(IdlTypeCode.LONG, 3)))));
    AnyException mismatch =
        assertThrows(
            AnyException.class,
            () ->
                pointCodec.write(
                    CdrWriter.bigEndian(),
                    new AnyAggregateValue(
                        pointType(),
                        Map.of(
                            "x",
                            new AnyValue<>(IdlTypeCode.STRING, "wrong"),
                            "y",
                            new AnyValue<>(IdlTypeCode.LONG, 2)))));

    assertEquals(AnyDiagnosticCodes.MISSING_MEMBER, missing.code());
    assertEquals(AnyDiagnosticCodes.UNKNOWN_MEMBER, extra.code());
    assertEquals(AnyDiagnosticCodes.TYPE_MISMATCH, mismatch.code());
  }

  @Test
  void codecFactoriesRejectUnsupportedTypeCodeCombinations() {
    IdlTypeCode serviceType =
        IdlTypeCode.fromDescriptor(
            new IdlGeneratedTypeDescriptor(
                IdlTypeKind.INTERFACE,
                "::demo::Service",
                "demo.Service",
                RepositoryId.parse("IDL:demo/Service:1.0"),
                List.of(),
                List.of(),
                List.of()));

    assertThrows(AnyException.class, () -> AnyCodecs.enumeration(serviceType));
    assertThrows(AnyException.class, () -> AnyCodecs.aggregate(serviceType, Map.of()));
    assertThrows(
        AnyException.class,
        () ->
            AnyCodecs.sequence(
                IdlTypeCode.sequenceOf(IdlTypeCode.STRING, "sequence<string>", "java.util.List"),
                AnyCodecs.longCodec()));
  }

  @Test
  void valuesAndCodecsDefensivelyCopyCallerOwnedCollections() {
    Map<String, AnyValue<?>> members =
        new java.util.LinkedHashMap<>(
            Map.of(
                "x",
                new AnyValue<>(IdlTypeCode.LONG, 1),
                "y",
                new AnyValue<>(IdlTypeCode.LONG, 2)));
    AnyAggregateValue aggregate = new AnyAggregateValue(pointType(), members);
    members.clear();

    assertEquals(2, aggregate.members().size());
    assertThrows(UnsupportedOperationException.class, () -> aggregate.members().clear());

    List<Integer> mutable = new ArrayList<>(List.of(7, 8));
    AnyValueCodec<List<Integer>> codec =
        AnyCodecs.sequence(
            IdlTypeCode.sequenceOf(IdlTypeCode.LONG, "sequence<long>", "java.util.List"),
            AnyCodecs.longCodec());
    AnyValue<List<Integer>> roundTripped = roundTrip(codec, mutable);
    mutable.clear();

    assertEquals(List.of(7, 8), roundTripped.value());
    assertThrows(UnsupportedOperationException.class, () -> roundTripped.value().clear());
  }

  @Test
  void mainSourcesDoNotIntroduceForbiddenDynamicOrTransportMechanisms() throws IOException {
    String source = productionSource("src/main/java");
    List<String> forbiddenTokens =
        List.of(
            "java.lang.reflect",
            "java.lang.ClassLoader",
            "java.lang.reflect.Proxy",
            "ObjectInputStream",
            "ObjectOutputStream",
            "io.github.mundanej.mjo.giop",
            "io.github.mundanej.mjo.iiop",
            "io.github.mundanej.mjo.orb",
            "io.github.mundanej.mjo.poa",
            "org.omg.");

    assertEquals(List.of(), forbiddenTokens.stream().filter(source::contains).toList());
  }

  private static <T> AnyValue<T> roundTrip(AnyValueCodec<T> codec, T value) {
    CdrWriter writer = CdrWriter.bigEndian();
    codec.writeAny(writer, new AnyValue<>(codec.typeCode(), value));
    CdrReader reader = CdrReader.bigEndian(writer.toByteArray());
    AnyValue<T> result = codec.readAny(reader);
    assertEquals(0, reader.remaining());
    return result;
  }

  private static byte[] sequenceBytes() {
    CdrWriter writer = CdrWriter.bigEndian();
    writer.writeSequenceLength(0);
    return writer.toByteArray();
  }

  private static AnyValueCodec<AnyAggregateValue> pointCodec() {
    return AnyCodecs.aggregate(
        pointType(), Map.of("x", AnyCodecs.longCodec(), "y", AnyCodecs.longCodec()));
  }

  private static AnyValueCodec<AnyAggregateValue> problemCodec() {
    return AnyCodecs.aggregate(problemType(), Map.of("message", AnyCodecs.stringCodec()));
  }

  private static IdlTypeCode pointType() {
    IdlTypeReference longType =
        new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", "int", Optional.empty());
    return IdlTypeCode.fromDescriptor(
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.STRUCT,
            "::demo::Point",
            "demo.Point",
            RepositoryId.parse("IDL:demo/Point:1.0"),
            List.of(new IdlFieldDescriptor("x", longType), new IdlFieldDescriptor("y", longType)),
            List.of(),
            List.of()));
  }

  private static IdlTypeCode colorType() {
    return IdlTypeCode.fromDescriptor(
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.ENUM,
            "::demo::Color",
            "demo.Color",
            RepositoryId.parse("IDL:demo/Color:1.0"),
            List.of(),
            List.of("RED", "GREEN"),
            List.of()));
  }

  private static IdlTypeCode problemType() {
    IdlTypeReference stringType =
        new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());
    return IdlTypeCode.fromDescriptor(
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.EXCEPTION,
            "::demo::Problem",
            "demo.Problem",
            RepositoryId.parse("IDL:demo/Problem:1.0"),
            List.of(new IdlFieldDescriptor("message", stringType)),
            List.of(),
            List.of()));
  }

  private static String productionSource(String root) throws IOException {
    try (Stream<Path> paths = Files.walk(Path.of(root))) {
      return paths
          .filter(path -> path.toString().endsWith(".java"))
          .map(AnyCodecTest::readString)
          .reduce("", String::concat);
    }
  }

  private static String readString(Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new AssertionError("failed to read source: " + path, exception);
    }
  }
}
