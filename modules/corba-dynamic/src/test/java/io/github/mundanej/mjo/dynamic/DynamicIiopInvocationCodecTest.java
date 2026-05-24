package io.github.mundanej.mjo.dynamic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.any.AnyCodecs;
import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.giop.GiopUserExceptionBody;
import io.github.mundanej.mjo.iiop.IiopClient;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOperationBinding;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopOrbClient;
import io.github.mundanej.mjo.iiop.IiopOrbServerHandler;
import io.github.mundanej.mjo.iiop.IiopServer;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Local IIOP integration tests for descriptor-backed dynamic invocation bodies. */
@Tag("unit")
final class DynamicIiopInvocationCodecTest {

  private static final IdlTypeReference CALCULATOR_REFERENCE =
      new IdlTypeReference(
          IdlTypeKind.INTERFACE,
          "::demo::Calculator",
          DynamicTestFixtures.Calculator.class.getName(),
          Optional.of(RepositoryId.parse("IDL:demo/Calculator:1.0")));
  private static final IdlTypeCode CALCULATOR_TYPE =
      IdlTypeCode.fromDescriptor(DynamicTestFixtures.SERVICE_DESCRIPTOR);
  private static final IdlOperationDescriptor ECHO_REFERENCE_OPERATION =
      new IdlOperationDescriptor(
          "echoReference",
          CALCULATOR_REFERENCE,
          List.of(new IdlParameterDescriptor("value", IdlParameterMode.IN, CALCULATOR_REFERENCE)),
          List.of());
  private static final IdlOperationDescriptor ADJUST_OPERATION =
      new IdlOperationDescriptor(
          "adjust",
          DynamicTestFixtures.LONG_REFERENCE,
          List.of(
              new IdlParameterDescriptor(
                  "seed", IdlParameterMode.IN, DynamicTestFixtures.LONG_REFERENCE),
              new IdlParameterDescriptor(
                  "doubled", IdlParameterMode.OUT, DynamicTestFixtures.LONG_REFERENCE),
              new IdlParameterDescriptor(
                  "total", IdlParameterMode.INOUT, DynamicTestFixtures.LONG_REFERENCE)),
          List.of());
  private static final IdlOperationDescriptor SAMPLE_OPERATION =
      new IdlOperationDescriptor(
          "sample",
          DynamicTestFixtures.VOID_REFERENCE,
          List.of(
              new IdlParameterDescriptor(
                  "value", IdlParameterMode.OUT, DynamicTestFixtures.LONG_REFERENCE)),
          List.of());

  @Test
  void dynamicInvocationCrossesLocalIiopLoopback() {
    DynamicOperationCodec codec = DynamicTestFixtures.addCodec();
    DynamicIiopInvocationCodec iiopCodec = new DynamicIiopInvocationCodec(codec);
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> localReference =
        bindCalculator(
            orb,
            request -> (Integer) request.arguments().get(0) + (Integer) request.arguments().get(1));

    try (IiopServer server =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            IiopOrbServerHandler.builder(orb)
                .bind(
                    localReference, List.of(new IiopOperationBinding(codec.operation(), iiopCodec)))
                .build())) {
      IiopObjectReference reference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      try (IiopOrbClient client = IiopOrbClient.connect(reference, IiopOptions.defaults())) {
        DynamicInvocationResult result =
            (DynamicInvocationResult)
                client.invoke(
                    codec.operation(),
                    iiopCodec,
                    List.of(DynamicTestFixtures.longAny(2), DynamicTestFixtures.longAny(5)));

        assertEquals(DynamicTestFixtures.ADD_OPERATION, result.operation());
        assertEquals(DynamicTestFixtures.longAny(7), result.value().orElseThrow());
      }
    }
  }

  @Test
  void voidDynamicInvocationHasEmptyIiopReplyBody() {
    DynamicOperationCodec codec = DynamicTestFixtures.pingCodec();
    DynamicIiopInvocationCodec iiopCodec = new DynamicIiopInvocationCodec(codec);
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> localReference =
        bindCalculator(orb, request -> null);

    try (IiopServer server =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            IiopOrbServerHandler.builder(orb)
                .bind(
                    localReference, List.of(new IiopOperationBinding(codec.operation(), iiopCodec)))
                .build())) {
      IiopObjectReference reference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      try (IiopOrbClient client = IiopOrbClient.connect(reference, IiopOptions.defaults())) {
        DynamicInvocationResult result =
            (DynamicInvocationResult) client.invoke(codec.operation(), iiopCodec, List.of());

        assertEquals(DynamicTestFixtures.PING_OPERATION, result.operation());
        assertEquals(Optional.empty(), result.value());
      }
    }
  }

  @Test
  void objectReferenceAnyValuesCrossDynamicIiopBodies() {
    DynamicOperationCodec codec =
        DynamicOperationCodec.valueReturn(
            ECHO_REFERENCE_OPERATION,
            AnyCodecs.objectReference(CALCULATOR_TYPE),
            List.of(AnyCodecs.objectReference(CALCULATOR_TYPE)));
    DynamicIiopInvocationCodec iiopCodec = new DynamicIiopInvocationCodec(codec);
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> localReference =
        orb.bind(
            DynamicTestFixtures.Calculator.class,
            echoDescriptor(),
            request -> request.arguments().get(0));
    Ior objectReference = Ior.nullReference();

    try (IiopServer server =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            IiopOrbServerHandler.builder(orb)
                .bind(
                    localReference, List.of(new IiopOperationBinding(codec.operation(), iiopCodec)))
                .build())) {
      IiopObjectReference reference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      try (IiopOrbClient client = IiopOrbClient.connect(reference, IiopOptions.defaults())) {
        DynamicInvocationResult result =
            (DynamicInvocationResult)
                client.invoke(
                    codec.operation(),
                    iiopCodec,
                    List.of(new AnyValue<>(CALCULATOR_TYPE, objectReference)));

        assertEquals(
            new AnyValue<>(CALCULATOR_TYPE, objectReference), result.value().orElseThrow());
      }
    }
  }

  @Test
  void outAndInoutValuesCrossDynamicIiopReplyBodies() {
    DynamicOperationCodec codec =
        DynamicOperationCodec.valueReturn(
            ADJUST_OPERATION,
            AnyCodecs.longCodec(),
            List.of(AnyCodecs.longCodec(), AnyCodecs.longCodec(), AnyCodecs.longCodec()));
    DynamicIiopInvocationCodec iiopCodec = new DynamicIiopInvocationCodec(codec);
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> localReference =
        orb.bind(
            DynamicTestFixtures.Calculator.class,
            adjustDescriptor(),
            request -> {
              assertEquals(List.of(3, 5), request.arguments());
              return DynamicInvocationResult.withOutValues(
                  ADJUST_OPERATION,
                  Optional.of(DynamicTestFixtures.longAny(13)),
                  List.of(DynamicTestFixtures.longAny(6), DynamicTestFixtures.longAny(8)));
            });

    try (IiopServer server =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            IiopOrbServerHandler.builder(orb)
                .bind(
                    localReference, List.of(new IiopOperationBinding(codec.operation(), iiopCodec)))
                .build())) {
      IiopObjectReference reference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      try (IiopOrbClient client = IiopOrbClient.connect(reference, IiopOptions.defaults())) {
        DynamicInvocationResult result =
            (DynamicInvocationResult)
                client.invoke(
                    codec.operation(),
                    iiopCodec,
                    List.of(DynamicTestFixtures.longAny(3), DynamicTestFixtures.longAny(5)));

        assertEquals(DynamicTestFixtures.longAny(13), result.value().orElseThrow());
        assertEquals(
            List.of(DynamicTestFixtures.longAny(6), DynamicTestFixtures.longAny(8)),
            result.outValues());
      }
    }
  }

  @Test
  void voidOperationsDecodeOutValuesFromNonEmptyReplyBodies() {
    DynamicOperationCodec codec =
        DynamicOperationCodec.voidReturn(SAMPLE_OPERATION, List.of(AnyCodecs.longCodec()));
    DynamicIiopInvocationCodec iiopCodec = new DynamicIiopInvocationCodec(codec);
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> localReference =
        orb.bind(
            DynamicTestFixtures.Calculator.class,
            sampleDescriptor(),
            request -> {
              assertEquals(List.of(), request.arguments());
              return DynamicInvocationResult.withOutValues(
                  SAMPLE_OPERATION, Optional.empty(), List.of(DynamicTestFixtures.longAny(21)));
            });

    try (IiopServer server =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            IiopOrbServerHandler.builder(orb)
                .bind(
                    localReference, List.of(new IiopOperationBinding(codec.operation(), iiopCodec)))
                .build())) {
      IiopObjectReference reference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      try (IiopOrbClient client = IiopOrbClient.connect(reference, IiopOptions.defaults())) {
        DynamicInvocationResult result =
            (DynamicInvocationResult) client.invoke(codec.operation(), iiopCodec, List.of());

        assertEquals(Optional.empty(), result.value());
        assertEquals(List.of(DynamicTestFixtures.longAny(21)), result.outValues());
      }
    }
  }

  @Test
  void rejectsMismatchedOperationsAndTrailingBodiesDeterministically() {
    DynamicIiopInvocationCodec codec =
        new DynamicIiopInvocationCodec(DynamicTestFixtures.addCodec());

    DynamicException wrongOperation =
        assertThrows(
            DynamicException.class,
            () -> codec.encodeArguments(DynamicTestFixtures.PING_OPERATION, List.of()));
    DynamicException trailingBody =
        assertThrows(
            DynamicException.class,
            () ->
                codec.decodeArguments(
                    DynamicTestFixtures.ADD_OPERATION,
                    io.github.mundanej.mjo.cdr.CdrWriter.bigEndian()
                        .writeLong(1)
                        .writeLong(2)
                        .writeOctet(3)
                        .toByteArray()));

    assertEquals(DynamicDiagnosticCodes.UNKNOWN_OPERATION, wrongOperation.code());
    assertEquals(DynamicDiagnosticCodes.INVALID_ARGUMENTS, trailingBody.code());
  }

  @Test
  void declaredUserExceptionsDecodeAsDynamicDiagnostics() {
    DynamicIiopInvocationCodec codec =
        new DynamicIiopInvocationCodec(DynamicTestFixtures.addCodec());

    RuntimeException exception =
        codec.decodeUserException(
            DynamicTestFixtures.ADD_OPERATION, "IDL:demo/Problem:1.0", new byte[0]);

    DynamicException dynamic = (DynamicException) exception;
    assertEquals(DynamicDiagnosticCodes.USER_EXCEPTION, dynamic.code());
  }

  @Test
  void declaredUserExceptionRepliesUseEmptyDynamicPayload() {
    DynamicOperationCodec codec = DynamicTestFixtures.addCodec();
    DynamicIiopInvocationCodec iiopCodec = new DynamicIiopInvocationCodec(codec);
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<DynamicTestFixtures.Calculator> localReference =
        bindCalculator(
            orb,
            request -> {
              throw new DynamicTestFixtures.DemoProblem();
            });

    try (IiopServer server =
            IiopServer.bind(
                IiopEndpoint.loopback(0),
                IiopOptions.defaults(),
                IiopOrbServerHandler.builder(orb)
                    .bind(
                        localReference,
                        List.of(new IiopOperationBinding(codec.operation(), iiopCodec)))
                    .build());
        IiopClient client = IiopClient.connect(server.endpoint(), IiopOptions.defaults())) {
      GiopReply reply =
          client.invoke(
              new GiopRequest(
                  GiopHeader.forType(GiopMessageType.REQUEST),
                  77,
                  3,
                  localReference.objectId().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                  DynamicTestFixtures.ADD_OPERATION.name(),
                  List.of(),
                  CdrWriter.bigEndian().writeLong(1).writeLong(2).toByteArray()));

      GiopUserExceptionBody body =
          GiopUserExceptionBody.fromBytes(CdrByteOrder.BIG_ENDIAN, reply.body());
      assertEquals(GiopReplyStatus.USER_EXCEPTION, reply.replyStatus());
      assertEquals("IDL:demo/Problem:1.0", body.repositoryId());
      assertEquals(0, body.payload().length);
    }
  }

  private static LocalObjectReference<DynamicTestFixtures.Calculator> bindCalculator(
      LocalOrb orb, ThrowingDispatcher dispatcher) {
    return orb.bind(
        DynamicTestFixtures.Calculator.class,
        DynamicTestFixtures.SERVICE_DESCRIPTOR,
        request -> dispatcher.invoke(request));
  }

  private static IdlGeneratedTypeDescriptor echoDescriptor() {
    return new IdlGeneratedTypeDescriptor(
        IdlTypeKind.INTERFACE,
        "::demo::Calculator",
        DynamicTestFixtures.Calculator.class.getName(),
        RepositoryId.parse("IDL:demo/Calculator:1.0"),
        List.of(),
        List.of(),
        List.of(ECHO_REFERENCE_OPERATION));
  }

  private static IdlGeneratedTypeDescriptor adjustDescriptor() {
    return new IdlGeneratedTypeDescriptor(
        IdlTypeKind.INTERFACE,
        "::demo::Calculator",
        DynamicTestFixtures.Calculator.class.getName(),
        RepositoryId.parse("IDL:demo/Calculator:1.0"),
        List.of(),
        List.of(),
        List.of(ADJUST_OPERATION));
  }

  private static IdlGeneratedTypeDescriptor sampleDescriptor() {
    return new IdlGeneratedTypeDescriptor(
        IdlTypeKind.INTERFACE,
        "::demo::Calculator",
        DynamicTestFixtures.Calculator.class.getName(),
        RepositoryId.parse("IDL:demo/Calculator:1.0"),
        List.of(),
        List.of(),
        List.of(SAMPLE_OPERATION));
  }

  @FunctionalInterface
  private interface ThrowingDispatcher {
    Object invoke(LocalInvocationRequest request) throws Exception;
  }
}
