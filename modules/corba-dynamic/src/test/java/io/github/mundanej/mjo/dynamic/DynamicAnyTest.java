package io.github.mundanej.mjo.dynamic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for local descriptor-backed DynamicAny values. */
@Tag("unit")
final class DynamicAnyTest {

  @Test
  void constructsPrimitiveEnumExceptionStructAndSequenceValues() {
    DynamicAny number = DynamicAnyFactory.value(IdlTypeCode.LONG, 7);
    DynamicAny color = DynamicAnyFactory.value(DynamicTestFixtures.colorType(), "GREEN");
    DynamicAny problem =
        DynamicAnyFactory.aggregate(
            DynamicTestFixtures.problemType(),
            Map.of("message", DynamicAnyFactory.value(IdlTypeCode.STRING, "failed")));
    DynamicAny point =
        DynamicAnyFactory.aggregate(
            DynamicTestFixtures.pointType(),
            Map.of(
                "x",
                DynamicAnyFactory.value(IdlTypeCode.LONG, 5),
                "label",
                DynamicAnyFactory.value(IdlTypeCode.STRING, "origin")));
    DynamicAny sequence =
        DynamicAnyFactory.sequence(
            IdlTypeCode.sequenceOf(IdlTypeCode.LONG, "sequence<long>", "java.util.List"),
            List.of(number));

    assertEquals(7, number.value());
    assertEquals("GREEN", color.value());
    assertEquals("failed", problem.member("message").value());
    assertEquals("origin", point.member("label").value());
    assertEquals(7, sequence.element(0).value());
  }

  @Test
  void aggregateAndSequenceUpdatesAreImmutableAndTypeChecked() {
    DynamicAny point =
        DynamicAnyFactory.aggregate(
            DynamicTestFixtures.pointType(),
            Map.of(
                "x",
                DynamicAnyFactory.value(IdlTypeCode.LONG, 5),
                "label",
                DynamicAnyFactory.value(IdlTypeCode.STRING, "origin")));
    DynamicAny updatedPoint =
        point.withMember("label", DynamicAnyFactory.value(IdlTypeCode.STRING, "updated"));
    DynamicAny sequence =
        DynamicAnyFactory.sequence(
            IdlTypeCode.sequenceOf(IdlTypeCode.LONG, "sequence<long>", "java.util.List"),
            List.of(DynamicAnyFactory.value(IdlTypeCode.LONG, 1)));
    DynamicAny updatedSequence =
        sequence.withElement(0, DynamicAnyFactory.value(IdlTypeCode.LONG, 9));

    assertEquals("origin", point.member("label").value());
    assertEquals("updated", updatedPoint.member("label").value());
    assertEquals(1, sequence.element(0).value());
    assertEquals(9, updatedSequence.element(0).value());
    assertThrows(
        DynamicException.class,
        () -> point.withMember("x", DynamicAnyFactory.value(IdlTypeCode.STRING, "bad")));
    assertThrows(
        DynamicException.class,
        () -> sequence.withElement(0, DynamicAnyFactory.value(IdlTypeCode.STRING, "bad")));
  }

  @Test
  void rejectsInvalidDynamicAnyValues() {
    DynamicException invalidEnum =
        assertThrows(
            DynamicException.class,
            () -> DynamicAnyFactory.value(DynamicTestFixtures.colorType(), "BLUE"));
    DynamicException wrongScalarPayload =
        assertThrows(
            DynamicException.class, () -> DynamicAnyFactory.value(IdlTypeCode.LONG, "bad"));
    DynamicException outOfRangeOctet =
        assertThrows(DynamicException.class, () -> DynamicAnyFactory.value(IdlTypeCode.OCTET, 256));
    DynamicException unsupportedInterface =
        assertThrows(
            DynamicException.class,
            () ->
                DynamicAnyFactory.fromAny(
                    new AnyValue<>(
                        IdlTypeCode.fromDescriptor(DynamicTestFixtures.SERVICE_DESCRIPTOR),
                        "bad")));
    DynamicException missingMember =
        assertThrows(
            DynamicException.class,
            () -> DynamicAnyFactory.aggregate(DynamicTestFixtures.pointType(), Map.of()));
    DynamicException wrongSequenceElement =
        assertThrows(
            DynamicException.class,
            () ->
                DynamicAnyFactory.sequence(
                    IdlTypeCode.sequenceOf(IdlTypeCode.LONG, "sequence<long>", "java.util.List"),
                    List.of(DynamicAnyFactory.value(IdlTypeCode.STRING, "bad"))));

    assertEquals(DynamicDiagnosticCodes.INVALID_ARGUMENTS, invalidEnum.code());
    assertEquals(DynamicDiagnosticCodes.TYPE_MISMATCH, wrongScalarPayload.code());
    assertEquals(DynamicDiagnosticCodes.TYPE_MISMATCH, outOfRangeOctet.code());
    assertEquals(DynamicDiagnosticCodes.UNSUPPORTED_TYPE, unsupportedInterface.code());
    assertEquals(DynamicDiagnosticCodes.INVALID_ARGUMENTS, missingMember.code());
    assertEquals(DynamicDiagnosticCodes.TYPE_MISMATCH, wrongSequenceElement.code());
    assertThrows(
        DynamicException.class,
        () -> DynamicAnyFactory.value(IdlTypeCode.LONG_DOUBLE, new byte[15]));
    assertThrows(DynamicException.class, () -> DynamicAnyFactory.value(IdlTypeCode.LONG, null));
  }

  @Test
  void wrapsExistingAnyAndRejectsNonAggregateNavigation() {
    DynamicAny number = DynamicAnyFactory.fromAny(new AnyValue<>(IdlTypeCode.LONG, 3));

    assertEquals(3, number.value());
    assertThrows(
        DynamicException.class,
        () -> {
          DynamicAny unexpected = number.member("missing");
          throw new AssertionError(unexpected);
        });
    assertThrows(
        DynamicException.class,
        () -> {
          DynamicAny unexpected = number.element(0);
          throw new AssertionError(unexpected);
        });
  }

  @Test
  void copiesCallerOwnedAggregateAndSequenceCollections() {
    Map<String, DynamicAny> members = new LinkedHashMap<>();
    members.put("x", DynamicAnyFactory.value(IdlTypeCode.LONG, 5));
    members.put("label", DynamicAnyFactory.value(IdlTypeCode.STRING, "origin"));
    DynamicAny point = DynamicAnyFactory.aggregate(DynamicTestFixtures.pointType(), members);

    List<DynamicAny> elements = new ArrayList<>();
    elements.add(DynamicAnyFactory.value(IdlTypeCode.LONG, 1));
    DynamicAny sequence =
        DynamicAnyFactory.sequence(
            IdlTypeCode.sequenceOf(IdlTypeCode.LONG, "sequence<long>", "java.util.List"), elements);

    members.put("label", DynamicAnyFactory.value(IdlTypeCode.STRING, "changed"));
    elements.set(0, DynamicAnyFactory.value(IdlTypeCode.LONG, 9));

    assertEquals("origin", point.member("label").value());
    assertEquals(1, sequence.element(0).value());
  }

  @Test
  void directConstructionRejectsUnsupportedTypeCodes() {
    DynamicException unsupportedInterface =
        assertThrows(
            DynamicException.class,
            () ->
                new DynamicAny(
                    new AnyValue<>(
                        IdlTypeCode.fromDescriptor(DynamicTestFixtures.SERVICE_DESCRIPTOR),
                        "bad")));
    DynamicException unsupportedVoid =
        assertThrows(
            DynamicException.class, () -> new DynamicAny(new AnyValue<>(IdlTypeCode.VOID, "bad")));

    assertEquals(DynamicDiagnosticCodes.UNSUPPORTED_TYPE, unsupportedInterface.code());
    assertEquals(DynamicDiagnosticCodes.UNSUPPORTED_TYPE, unsupportedVoid.code());
  }
}
