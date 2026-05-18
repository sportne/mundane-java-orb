package io.github.mundanej.mjo.dynamic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.any.AnyAggregateValue;
import io.github.mundanej.mjo.any.AnyCodecs;
import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.any.AnyValueCodec;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.OBJECT_NOT_EXIST;

/** Integration tests for local DII-style dynamic invocation. */
@Tag("unit")
final class DynamicInvocationTest {

  @Test
  void invokesLocalOrbReferenceWithDynamicArgumentsAndReturnValue() {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> reference =
        bindCalculator(
            orb,
            request -> (Integer) request.arguments().get(0) + (Integer) request.arguments().get(1));
    DynamicOperationCodec codec = DynamicTestFixtures.addCodec();
    DynamicInvocationRequest request =
        new DynamicInvocationRequest(
            codec, List.of(DynamicTestFixtures.longAny(2), DynamicTestFixtures.longAny(5)));

    DynamicInvocationResult result = new DynamicInvoker(orb).invoke(reference, request);

    assertEquals(DynamicTestFixtures.ADD_OPERATION, result.operation());
    assertEquals(
        new AnyValue<>(codec.returnCodec().orElseThrow().typeCode(), 7),
        result.value().orElseThrow());
  }

  @Test
  void invokesVoidLocalOperation() {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> reference =
        bindCalculator(orb, request -> null);
    DynamicInvocationRequest request =
        new DynamicInvocationRequest(DynamicTestFixtures.pingCodec(), List.of());

    DynamicInvocationResult result = new DynamicInvoker(orb).invoke(reference, request);

    assertEquals(DynamicTestFixtures.PING_OPERATION, result.operation());
    assertEquals(java.util.Optional.empty(), result.value());
  }

  @Test
  void rejectsInvalidDynamicInvocationRequests() {
    DynamicOperationCodec codec = DynamicTestFixtures.addCodec();
    IdlOperationDescriptor undeclared =
        new IdlOperationDescriptor(
            "missing", DynamicTestFixtures.LONG_REFERENCE, List.of(), List.of());
    DynamicOperationCodec undeclaredCodec =
        DynamicOperationCodec.valueReturn(
            undeclared, io.github.mundanej.mjo.any.AnyCodecs.longCodec(), List.of());
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> reference =
        bindCalculator(orb, request -> 0);

    assertThrows(
        DynamicException.class,
        () -> new DynamicInvocationRequest(codec, List.of(DynamicTestFixtures.longAny(1))));
    assertThrows(
        DynamicException.class,
        () ->
            new DynamicInvocationRequest(
                codec,
                List.of(
                    new AnyValue<>(io.github.mundanej.mjo.typecode.IdlTypeCode.STRING, "bad"),
                    DynamicTestFixtures.longAny(1))));
    DynamicException wrongPayload =
        assertThrows(
            DynamicException.class,
            () ->
                new DynamicInvocationRequest(
                    codec,
                    List.of(
                        new AnyValue<>(io.github.mundanej.mjo.typecode.IdlTypeCode.LONG, "bad"),
                        DynamicTestFixtures.longAny(1))));
    assertThrows(DynamicException.class, DynamicTestFixtures::unsupportedOutOperation);
    assertThrows(
        BAD_OPERATION.class,
        () ->
            new DynamicInvoker(orb)
                .invoke(reference, new DynamicInvocationRequest(undeclaredCodec, List.of())));

    assertEquals(DynamicDiagnosticCodes.TYPE_MISMATCH, wrongPayload.code());
  }

  @Test
  void rejectsInvalidGeneratedStyleReturnPayloads() {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> reference =
        bindCalculator(orb, request -> "bad");
    DynamicInvocationRequest request =
        new DynamicInvocationRequest(
            DynamicTestFixtures.addCodec(),
            List.of(DynamicTestFixtures.longAny(1), DynamicTestFixtures.longAny(2)));

    DynamicException exception =
        assertThrows(
            DynamicException.class, () -> new DynamicInvoker(orb).invoke(reference, request));

    assertEquals(DynamicDiagnosticCodes.TYPE_MISMATCH, exception.code());
  }

  @Test
  void propagatesLocalOrbLifecycleAndMissingTargetFailures() {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> reference =
        bindCalculator(orb, request -> 0);
    DynamicInvocationRequest request =
        new DynamicInvocationRequest(
            DynamicTestFixtures.addCodec(),
            List.of(DynamicTestFixtures.longAny(1), DynamicTestFixtures.longAny(2)));

    orb.unbind(reference.objectId());
    assertThrows(OBJECT_NOT_EXIST.class, () -> new DynamicInvoker(orb).invoke(reference, request));

    LocalOrb shutdownOrb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> shutdownReference =
        bindCalculator(shutdownOrb, localRequest -> 0);
    shutdownOrb.shutdown();
    assertThrows(
        BAD_INV_ORDER.class,
        () -> new DynamicInvoker(shutdownOrb).invoke(shutdownReference, request));
  }

  @Test
  void wrapsDeclaredUserExceptionsWithoutReflection() {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> reference =
        bindCalculator(
            orb,
            request -> {
              throw new DynamicTestFixtures.DemoProblem();
            });
    DynamicInvocationRequest request =
        new DynamicInvocationRequest(
            DynamicTestFixtures.addCodec(),
            List.of(DynamicTestFixtures.longAny(1), DynamicTestFixtures.longAny(2)));

    DynamicUserException exception =
        assertThrows(
            DynamicUserException.class, () -> new DynamicInvoker(orb).invoke(reference, request));

    assertEquals(DynamicDiagnosticCodes.USER_EXCEPTION, exception.code());
    assertEquals(DynamicTestFixtures.ADD_OPERATION, exception.operation());
    assertEquals(IdlTypeKind.EXCEPTION, exception.raisedType().kind());
  }

  @Test
  void validatesSupportedGeneratedStylePayloadShapes() {
    IdlTypeCode sequenceType =
        IdlTypeCode.sequenceOf(IdlTypeCode.LONG, "sequence<long>", "java.util.List");
    List<AnyValueCodec<?>> codecs =
        List.of(
            AnyCodecs.booleanCodec(),
            AnyCodecs.octetCodec(),
            AnyCodecs.charCodec(),
            AnyCodecs.shortCodec(),
            AnyCodecs.unsignedShortCodec(),
            AnyCodecs.longCodec(),
            AnyCodecs.unsignedLongCodec(),
            AnyCodecs.longLongCodec(),
            AnyCodecs.unsignedLongLongCodec(),
            AnyCodecs.floatCodec(),
            AnyCodecs.doubleCodec(),
            AnyCodecs.longDoubleCodec(),
            AnyCodecs.stringCodec(),
            AnyCodecs.enumeration(DynamicTestFixtures.colorType()),
            AnyCodecs.aggregate(
                DynamicTestFixtures.pointType(),
                Map.of("x", AnyCodecs.longCodec(), "label", AnyCodecs.stringCodec())),
            AnyCodecs.sequence(sequenceType, AnyCodecs.longCodec()));
    DynamicOperationCodec codec =
        DynamicOperationCodec.voidReturn(payloadOperation(codecs), codecs);
    byte[] longDouble = new byte[16];
    AnyAggregateValue point =
        new AnyAggregateValue(
            DynamicTestFixtures.pointType(),
            Map.of(
                "x",
                DynamicTestFixtures.longAny(1),
                "label",
                new AnyValue<>(IdlTypeCode.STRING, "origin")));

    List<AnyValue<?>> values =
        codec.toAnyArguments(
            List.of(
                true,
                7,
                'a',
                (short) 8,
                9,
                10,
                11L,
                12L,
                BigInteger.valueOf(13L),
                1.25f,
                2.5d,
                longDouble,
                "text",
                "RED",
                point,
                List.of(1, 2)));

    assertEquals(codecs.size(), values.size());
    assertEquals(longDouble[0], ((byte[]) values.get(11).value())[0]);
  }

  @Test
  void rejectsInvalidGeneratedStyleAggregateAndSequencePayloads() {
    IdlTypeCode sequenceType =
        IdlTypeCode.sequenceOf(IdlTypeCode.LONG, "sequence<long>", "java.util.List");
    DynamicOperationCodec aggregateCodec =
        DynamicOperationCodec.voidReturn(
            payloadOperation(
                List.of(
                    AnyCodecs.aggregate(
                        DynamicTestFixtures.pointType(),
                        Map.of("x", AnyCodecs.longCodec(), "label", AnyCodecs.stringCodec())))),
            List.of(
                AnyCodecs.aggregate(
                    DynamicTestFixtures.pointType(),
                    Map.of("x", AnyCodecs.longCodec(), "label", AnyCodecs.stringCodec()))));
    DynamicOperationCodec sequenceCodec =
        DynamicOperationCodec.voidReturn(
            payloadOperation(List.of(AnyCodecs.sequence(sequenceType, AnyCodecs.longCodec()))),
            List.of(AnyCodecs.sequence(sequenceType, AnyCodecs.longCodec())));
    AnyAggregateValue malformedPoint =
        new AnyAggregateValue(
            DynamicTestFixtures.pointType(),
            Map.of(
                "x",
                new AnyValue<>(IdlTypeCode.LONG, "bad"),
                "label",
                new AnyValue<>(IdlTypeCode.STRING, "origin")));

    assertThrows(DynamicException.class, () -> aggregateCodec.toAnyArguments(List.of("bad")));
    assertThrows(
        DynamicException.class, () -> aggregateCodec.toAnyArguments(List.of(malformedPoint)));
    assertThrows(DynamicException.class, () -> sequenceCodec.toAnyArguments(List.of("bad")));
    assertThrows(
        DynamicException.class, () -> sequenceCodec.toAnyArguments(List.of(List.of("bad"))));
  }

  private static LocalObjectReference<DynamicTestFixtures.Calculator> bindCalculator(
      LocalOrb orb, ThrowingDispatcher dispatcher) {
    return orb.bind(
        DynamicTestFixtures.Calculator.class,
        DynamicTestFixtures.SERVICE_DESCRIPTOR,
        request -> dispatcher.invoke(request));
  }

  private static IdlOperationDescriptor payloadOperation(List<AnyValueCodec<?>> codecs) {
    List<IdlParameterDescriptor> parameters = new ArrayList<>(codecs.size());
    for (int index = 0; index < codecs.size(); index++) {
      parameters.add(
          new IdlParameterDescriptor(
              "p" + index, IdlParameterMode.IN, DynamicTestFixtures.LONG_REFERENCE));
    }
    return new IdlOperationDescriptor(
        "payload", DynamicTestFixtures.VOID_REFERENCE, parameters, List.of());
  }

  @FunctionalInterface
  private interface ThrowingDispatcher {
    Object invoke(LocalInvocationRequest request) throws Exception;
  }
}
