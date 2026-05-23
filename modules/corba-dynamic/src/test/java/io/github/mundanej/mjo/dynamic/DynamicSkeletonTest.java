package io.github.mundanej.mjo.dynamic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for local DSI-style dynamic skeleton dispatch. */
@Tag("unit")
final class DynamicSkeletonTest {

  @Test
  void adaptsGeneratedStyleRequestToDynamicHandler() throws Exception {
    LocalInvocationDispatcher dispatcher =
        DynamicSkeleton.dispatcher(
            DynamicTestFixtures.SERVICE_DESCRIPTOR,
            List.of(DynamicTestFixtures.addCodec()),
            request -> {
              assertEquals(DynamicTestFixtures.ADD_OPERATION, request.operationCodec().operation());
              assertEquals(
                  List.of(2, 3), request.operationCodec().toPayloadArguments(request.arguments()));
              return DynamicInvocationResult.value(
                  request.operationCodec().operation(), DynamicTestFixtures.longAny(5));
            });

    Object result =
        dispatcher.invoke(
            new LocalInvocationRequest(
                DynamicTestFixtures.SERVICE_DESCRIPTOR,
                DynamicTestFixtures.ADD_OPERATION,
                List.of(2, 3)));

    assertEquals(5, result);
  }

  @Test
  void adaptsVoidDynamicResultBackToGeneratedStyleNull() throws Exception {
    LocalInvocationDispatcher dispatcher =
        DynamicSkeleton.dispatcher(
            DynamicTestFixtures.SERVICE_DESCRIPTOR,
            List.of(DynamicTestFixtures.pingCodec()),
            request -> DynamicInvocationResult.voidResult(request.operationCodec().operation()));

    Object result =
        dispatcher.invoke(
            new LocalInvocationRequest(
                DynamicTestFixtures.SERVICE_DESCRIPTOR,
                DynamicTestFixtures.PING_OPERATION,
                List.of()));

    assertEquals(null, result);
  }

  @Test
  void rejectsUnknownOperationsAndHandlerTypeErrors() {
    LocalInvocationDispatcher dispatcher =
        DynamicSkeleton.dispatcher(
            DynamicTestFixtures.SERVICE_DESCRIPTOR,
            List.of(DynamicTestFixtures.addCodec()),
            request ->
                DynamicInvocationResult.value(
                    request.operationCodec().operation(),
                    new io.github.mundanej.mjo.any.AnyValue<>(
                        io.github.mundanej.mjo.typecode.IdlTypeCode.STRING, "bad")));
    LocalInvocationRequest pingRequest =
        new LocalInvocationRequest(
            DynamicTestFixtures.SERVICE_DESCRIPTOR, DynamicTestFixtures.PING_OPERATION, List.of());
    LocalInvocationRequest addRequest =
        new LocalInvocationRequest(
            DynamicTestFixtures.SERVICE_DESCRIPTOR,
            DynamicTestFixtures.ADD_OPERATION,
            List.of(1, 2));

    assertThrows(DynamicException.class, () -> dispatcher.invoke(pingRequest));
    assertThrows(DynamicException.class, () -> dispatcher.invoke(addRequest));
  }

  @Test
  void rejectsInvalidDynamicHandlerReturnPayloads() {
    LocalInvocationDispatcher dispatcher =
        DynamicSkeleton.dispatcher(
            DynamicTestFixtures.SERVICE_DESCRIPTOR,
            List.of(DynamicTestFixtures.addCodec()),
            request ->
                DynamicInvocationResult.value(
                    request.operationCodec().operation(),
                    new io.github.mundanej.mjo.any.AnyValue<>(
                        io.github.mundanej.mjo.typecode.IdlTypeCode.LONG, "bad")));
    LocalInvocationRequest request =
        new LocalInvocationRequest(
            DynamicTestFixtures.SERVICE_DESCRIPTOR,
            DynamicTestFixtures.ADD_OPERATION,
            List.of(1, 2));

    DynamicException exception =
        assertThrows(DynamicException.class, () -> dispatcher.invoke(request));

    assertEquals(DynamicDiagnosticCodes.TYPE_MISMATCH, exception.code());
  }

  @Test
  void rejectsDynamicHandlerResultShapeMismatches() {
    LocalInvocationDispatcher missingValue =
        DynamicSkeleton.dispatcher(
            DynamicTestFixtures.SERVICE_DESCRIPTOR,
            List.of(DynamicTestFixtures.addCodec()),
            request -> DynamicInvocationResult.voidResult(request.operationCodec().operation()));
    LocalInvocationDispatcher voidWithValue =
        DynamicSkeleton.dispatcher(
            DynamicTestFixtures.SERVICE_DESCRIPTOR,
            List.of(DynamicTestFixtures.pingCodec()),
            request ->
                DynamicInvocationResult.value(
                    request.operationCodec().operation(), DynamicTestFixtures.longAny(1)));

    DynamicException missingValueFailure =
        assertThrows(
            DynamicException.class,
            () ->
                missingValue.invoke(
                    new LocalInvocationRequest(
                        DynamicTestFixtures.SERVICE_DESCRIPTOR,
                        DynamicTestFixtures.ADD_OPERATION,
                        List.of(1, 2))));
    DynamicException voidWithValueFailure =
        assertThrows(
            DynamicException.class,
            () ->
                voidWithValue.invoke(
                    new LocalInvocationRequest(
                        DynamicTestFixtures.SERVICE_DESCRIPTOR,
                        DynamicTestFixtures.PING_OPERATION,
                        List.of())));

    assertEquals(DynamicDiagnosticCodes.TYPE_MISMATCH, missingValueFailure.code());
    assertEquals(DynamicDiagnosticCodes.TYPE_MISMATCH, voidWithValueFailure.code());
  }

  @Test
  void rejectsInvalidGeneratedStyleArgumentsBeforeDynamicHandlerRuns() {
    LocalInvocationDispatcher dispatcher =
        DynamicSkeleton.dispatcher(
            DynamicTestFixtures.SERVICE_DESCRIPTOR,
            List.of(DynamicTestFixtures.addCodec()),
            request ->
                DynamicInvocationResult.value(
                    request.operationCodec().operation(), DynamicTestFixtures.longAny(0)));
    LocalInvocationRequest request =
        new LocalInvocationRequest(
            DynamicTestFixtures.SERVICE_DESCRIPTOR,
            DynamicTestFixtures.ADD_OPERATION,
            List.of("bad", 2));

    DynamicException exception =
        assertThrows(DynamicException.class, () -> dispatcher.invoke(request));

    assertEquals(DynamicDiagnosticCodes.TYPE_MISMATCH, exception.code());
  }

  @Test
  void rejectsMismatchedTargetDescriptor() {
    IdlOperationDescriptor operation = DynamicTestFixtures.ADD_OPERATION;
    LocalInvocationDispatcher dispatcher =
        DynamicSkeleton.dispatcher(
            DynamicTestFixtures.SERVICE_DESCRIPTOR,
            List.of(DynamicTestFixtures.addCodec()),
            request -> DynamicInvocationResult.voidResult(operation));
    LocalInvocationRequest request =
        new LocalInvocationRequest(
            new io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor(
                io.github.mundanej.mjo.typecode.IdlTypeKind.INTERFACE,
                "::demo::Other",
                "demo.Other",
                io.github.mundanej.mjo.repositoryid.RepositoryId.parse("IDL:demo/Other:1.0"),
                List.of(),
                List.of(),
                List.of(operation)),
            operation,
            List.of(1, 2));

    assertThrows(DynamicException.class, () -> dispatcher.invoke(request));
  }
}
