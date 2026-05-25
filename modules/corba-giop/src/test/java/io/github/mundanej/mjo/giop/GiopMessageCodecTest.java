package io.github.mundanej.mjo.giop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.IiopVersion;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.IorCodeSetComponent;
import io.github.mundanej.mjo.ior.ObjectKey;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.testkit.GoldenAssertions;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit and golden-wire tests for the bounded GIOP 1.2 message codec. */
@Tag("unit")
final class GiopMessageCodecTest {

  private final GiopMessageReader reader = new GiopMessageReader();
  private final GiopMessageWriter writer = new GiopMessageWriter();

  @Test
  void requestRoundTripsWithKeyAddrGoldenWire() {
    GiopRequest request =
        new GiopRequest(
            GiopHeader.forType(GiopMessageType.REQUEST),
            1,
            3,
            bytes(0x4B),
            "a",
            List.of(),
            bytes());
    byte[] expected =
        bytes(
            0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00,
            0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
            0x4B, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x61, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00);

    GoldenAssertions.assertBytesEquals("giop-request-keyaddr", expected, writer.write(request));

    GiopRequest decoded = assertInstanceOf(GiopRequest.class, reader.read(expected));
    assertEquals(1, decoded.requestId());
    assertEquals(3, decoded.responseFlags());
    assertArrayEquals(bytes(0x4B), decoded.objectKey());
    assertEquals("a", decoded.operation());
    assertEquals(List.of(), decoded.serviceContexts());
    assertArrayEquals(bytes(), decoded.body());
  }

  @Test
  void replyRoundTripsGoldenWireAndAllLocalStatuses() {
    GiopReply reply =
        new GiopReply(
            GiopHeader.forType(GiopMessageType.REPLY),
            2,
            GiopReplyStatus.NO_EXCEPTION,
            List.of(),
            bytes(0xAA));
    byte[] expected =
        bytes(
            0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x01, 0x00, 0x00, 0x00, 0x0D, 0x00, 0x00,
            0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xAA);

    GoldenAssertions.assertBytesEquals("giop-reply", expected, writer.write(reply));

    GiopReply decoded = assertInstanceOf(GiopReply.class, reader.read(expected));
    assertEquals(GiopReplyStatus.NO_EXCEPTION, decoded.replyStatus());
    assertArrayEquals(bytes(0xAA), decoded.body());

    for (GiopReplyStatus status : GiopReplyStatus.values()) {
      GiopReply statusReply =
          new GiopReply(GiopHeader.forType(GiopMessageType.REPLY), 6, status, List.of(), bytes());
      GiopReply statusDecoded =
          assertInstanceOf(GiopReply.class, reader.read(writer.write(statusReply)));
      assertEquals(status, statusDecoded.replyStatus());
    }
  }

  @Test
  void requestAndReplyOperationBodiesAreEightAlignedOnTheWire() {
    GiopRequest request =
        new GiopRequest(
            GiopHeader.forType(GiopMessageType.REQUEST),
            9,
            3,
            bytes(0x4B),
            "add",
            List.of(),
            bytes(0x00, 0x00, 0x00, 0x0D));
    byte[] requestWire = writer.write(request);
    int requestBodyOffset = indexOf(requestWire, bytes(0x00, 0x00, 0x00, 0x0D));
    assertEquals(0, requestBodyOffset % 8);
    assertEquals(48, requestBodyOffset);
    GiopRequest decodedRequest = assertInstanceOf(GiopRequest.class, reader.read(requestWire));
    assertArrayEquals(bytes(0x00, 0x00, 0x00, 0x0D), decodedRequest.body());

    GiopReply reply =
        new GiopReply(
            GiopHeader.forType(GiopMessageType.REPLY),
            9,
            GiopReplyStatus.NO_EXCEPTION,
            List.of(),
            bytes(0x00, 0x00, 0x00, 0x2A));
    byte[] replyWire = writer.write(reply);
    int replyBodyOffset = indexOf(replyWire, bytes(0x00, 0x00, 0x00, 0x2A));
    assertEquals(0, replyBodyOffset % 8);
    assertEquals(24, replyBodyOffset);
    GiopReply decodedReply = assertInstanceOf(GiopReply.class, reader.read(replyWire));
    assertArrayEquals(bytes(0x00, 0x00, 0x00, 0x2A), decodedReply.body());
  }

  @Test
  void cancelRequestRoundTripsGoldenWire() {
    GiopCancelRequest cancelRequest =
        new GiopCancelRequest(GiopHeader.forType(GiopMessageType.CANCEL_REQUEST), 5);
    byte[] expected =
        bytes(
            0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x02, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00,
            0x00, 0x05);

    GoldenAssertions.assertBytesEquals(
        "giop-cancel-request", expected, writer.write(cancelRequest));

    GiopCancelRequest decoded = assertInstanceOf(GiopCancelRequest.class, reader.read(expected));
    assertEquals(5, decoded.requestId());
  }

  @Test
  void littleEndianHeaderFlagsAndSizeDriveBodyByteOrder() {
    GiopCancelRequest cancelRequest =
        new GiopCancelRequest(
            new GiopHeader(GiopVersion.GIOP_1_2, true, false, GiopMessageType.CANCEL_REQUEST, 0),
            0x01020304L);
    byte[] expected =
        bytes(
            0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x01, 0x02, 0x04, 0x00, 0x00, 0x00, 0x04, 0x03,
            0x02, 0x01);

    GoldenAssertions.assertBytesEquals(
        "giop-cancel-request-little-endian", expected, writer.write(cancelRequest));

    GiopCancelRequest decoded = assertInstanceOf(GiopCancelRequest.class, reader.read(expected));
    assertTrue(decoded.header().littleEndian());
    assertEquals(0x01020304L, decoded.requestId());
  }

  @Test
  void locateRequestRoundTripsWithKeyAddrGoldenWire() {
    GiopLocateRequest locateRequest =
        new GiopLocateRequest(GiopHeader.forType(GiopMessageType.LOCATE_REQUEST), 3, bytes(0x4C));
    byte[] expected =
        bytes(
            0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x03, 0x00, 0x00, 0x00, 0x0D, 0x00, 0x00,
            0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x4C);

    GoldenAssertions.assertBytesEquals(
        "giop-locate-request-keyaddr", expected, writer.write(locateRequest));

    GiopLocateRequest decoded = assertInstanceOf(GiopLocateRequest.class, reader.read(expected));
    assertEquals(3, decoded.requestId());
    assertArrayEquals(bytes(0x4C), decoded.objectKey());
  }

  @Test
  void locateReplyRoundTripsGoldenWireAndAllLocalStatuses() {
    GiopLocateReply locateReply =
        new GiopLocateReply(
            GiopHeader.forType(GiopMessageType.LOCATE_REPLY),
            4,
            GiopLocateStatus.OBJECT_HERE,
            bytes(0x99));
    byte[] expected =
        bytes(
            0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00,
            0x00, 0x04, 0x00, 0x00, 0x00, 0x01, 0x99);

    GoldenAssertions.assertBytesEquals("giop-locate-reply", expected, writer.write(locateReply));

    GiopLocateReply decoded = assertInstanceOf(GiopLocateReply.class, reader.read(expected));
    assertEquals(GiopLocateStatus.OBJECT_HERE, decoded.locateStatus());
    assertArrayEquals(bytes(0x99), decoded.body());

    for (GiopLocateStatus status : GiopLocateStatus.values()) {
      GiopLocateReply statusReply =
          new GiopLocateReply(GiopHeader.forType(GiopMessageType.LOCATE_REPLY), 7, status, bytes());
      GiopLocateReply statusDecoded =
          assertInstanceOf(GiopLocateReply.class, reader.read(writer.write(statusReply)));
      assertEquals(status, statusDecoded.locateStatus());
    }
  }

  @Test
  void closeConnectionAndMessageErrorRoundTripEmptyBodiesGoldenWire() {
    GiopCloseConnection closeConnection =
        new GiopCloseConnection(GiopHeader.forType(GiopMessageType.CLOSE_CONNECTION));
    byte[] closeExpected =
        bytes(0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00);

    GiopMessageError messageError =
        new GiopMessageError(GiopHeader.forType(GiopMessageType.MESSAGE_ERROR));
    byte[] errorExpected =
        bytes(0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00);

    GoldenAssertions.assertBytesEquals(
        "giop-close-connection", closeExpected, writer.write(closeConnection));
    GoldenAssertions.assertBytesEquals(
        "giop-message-error", errorExpected, writer.write(messageError));
    assertInstanceOf(GiopCloseConnection.class, reader.read(closeExpected));
    assertInstanceOf(GiopMessageError.class, reader.read(errorExpected));
    assertGiopCode(
        GiopDiagnosticCodes.INVALID_BODY,
        () ->
            reader.read(
                bytes(
                    0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x05, 0x00, 0x00, 0x00, 0x01, 0x00)));
  }

  @Test
  void fragmentRoundTripsRequestIdMoreFragmentsFlagAndPayloadGoldenWire() {
    GiopFragment fragment =
        new GiopFragment(
            new GiopHeader(GiopVersion.GIOP_1_2, false, true, GiopMessageType.FRAGMENT, 0),
            8,
            bytes(0xF0, 0xF1));
    byte[] expected =
        bytes(
            0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x02, 0x07, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00,
            0x00, 0x08, 0xF0, 0xF1);

    GoldenAssertions.assertBytesEquals("giop-fragment", expected, writer.write(fragment));

    GiopFragment decoded = assertInstanceOf(GiopFragment.class, reader.read(expected));
    assertEquals(8, decoded.requestId());
    assertTrue(decoded.header().moreFragments());
    assertArrayEquals(bytes(0xF0, 0xF1), decoded.fragmentPayload());
  }

  @Test
  void profileAddrAndReferenceAddrTargetsRoundTrip() {
    TaggedProfile profile =
        TaggedProfile.internetIop(
            new IiopProfile(
                IiopVersion.V1_2, "host.example", 2809, new ObjectKey(bytes(0x4B)), List.of()));
    Ior ior = new Ior("IDL:demo/Service:1.0", List.of(profile));

    GiopRequest profileRequest =
        new GiopRequest(
            GiopHeader.forType(GiopMessageType.REQUEST),
            11,
            3,
            GiopTargetAddress.profileAddr(profile),
            "op",
            List.of(),
            bytes());
    GiopLocateRequest referenceLocate =
        new GiopLocateRequest(
            GiopHeader.forType(GiopMessageType.LOCATE_REQUEST),
            12,
            GiopTargetAddress.referenceAddr(0, ior));

    GiopRequest decodedProfile =
        assertInstanceOf(GiopRequest.class, reader.read(writer.write(profileRequest)));
    GiopLocateRequest decodedReference =
        assertInstanceOf(GiopLocateRequest.class, reader.read(writer.write(referenceLocate)));

    assertEquals(GiopTargetAddress.PROFILE_ADDR, decodedProfile.targetAddress().discriminator());
    assertEquals(profile, decodedProfile.targetAddress().profile());
    assertEquals(
        GiopTargetAddress.REFERENCE_ADDR, decodedReference.targetAddress().discriminator());
    assertEquals(0, decodedReference.targetAddress().selectedProfileIndex());
    assertEquals(ior, decodedReference.targetAddress().ior());
    assertThrows(GiopException.class, decodedProfile::objectKey);
  }

  @Test
  void replyExceptionBodiesAndCodeSetContextsRoundTrip() {
    GiopSystemExceptionBody system =
        new GiopSystemExceptionBody(
            "IDL:omg.org/CORBA/NO_IMPLEMENT:1.0", 7, GiopCompletionStatus.COMPLETED_NO);
    GiopUserExceptionBody user = new GiopUserExceptionBody("IDL:demo/Problem:1.0", bytes(0xCA));
    GiopCodeSetContext codeSets = GiopCodeSetContext.defaults();

    assertEquals(
        system,
        GiopSystemExceptionBody.fromBytes(
            CdrByteOrder.BIG_ENDIAN, system.toBytes(CdrByteOrder.BIG_ENDIAN)));
    assertEquals(
        user,
        GiopUserExceptionBody.fromBytes(
            CdrByteOrder.BIG_ENDIAN, user.toBytes(CdrByteOrder.BIG_ENDIAN)));
    assertEquals(codeSets, GiopCodeSetContext.fromServiceContext(codeSets.toServiceContext()));
    assertEquals(
        new GiopCodeSetContext(IorCodeSetComponent.UTF_8, IorCodeSetComponent.UTF_16),
        GiopCodeSetContext.fromServiceContext(
            new GiopCodeSetContext(IorCodeSetComponent.UTF_8, IorCodeSetComponent.UTF_16)
                .toServiceContext()));
    assertGiopCode(
        GiopDiagnosticCodes.UNSUPPORTED_BODY,
        () -> new GiopCodeSetContext(0xFFFF_FFFFL, IorCodeSetComponent.UTF_16));
  }

  @Test
  void fragmentAssemblerCombinesBoundedPayloadsAndRejectsMismatches() {
    GiopRequest first =
        new GiopRequest(
            new GiopHeader(GiopVersion.GIOP_1_2, false, true, GiopMessageType.REQUEST, 0),
            33,
            3,
            bytes(0x4B),
            "op",
            List.of(),
            bytes(0x01));
    GiopFragment finalFragment =
        new GiopFragment(GiopHeader.forType(GiopMessageType.FRAGMENT), 33, bytes(0x02, 0x03));

    GiopRequest assembled =
        assertInstanceOf(
            GiopRequest.class, new GiopFragmentAssembler().assemble(List.of(first, finalFragment)));

    assertArrayEquals(bytes(0x01, 0x02, 0x03), assembled.body());
    assertTrue(!assembled.header().moreFragments());
    assertGiopCode(
        GiopDiagnosticCodes.INVALID_BODY,
        () ->
            new GiopFragmentAssembler()
                .assemble(
                    List.of(
                        first,
                        new GiopFragment(
                            GiopHeader.forType(GiopMessageType.FRAGMENT), 34, bytes(0x02)))));

    GiopLimits strict =
        new GiopLimits(
            new BoundedLimit("message", 128),
            new BoundedLimit("body", 128),
            new BoundedLimit("contexts", 1),
            new BoundedLimit("context-data", 1),
            new BoundedLimit("fragments", 1),
            new BoundedLimit("fragment-body", 1));
    assertGiopCode(
        GiopDiagnosticCodes.LIMIT_EXCEEDED,
        () -> new GiopFragmentAssembler(strict).assemble(List.of(first, finalFragment)));
  }

  @Test
  void serviceContextsAreOpaqueBoundedAndDefensivelyCopied() {
    byte[] contextData = bytes(0x01, 0x02);
    GiopServiceContext serviceContext = new GiopServiceContext(42, contextData);
    contextData[0] = 0x7F;
    GiopReply reply =
        new GiopReply(
            GiopHeader.forType(GiopMessageType.REPLY),
            10,
            GiopReplyStatus.NO_EXCEPTION,
            List.of(serviceContext),
            bytes());

    GiopReply decoded = assertInstanceOf(GiopReply.class, reader.read(writer.write(reply)));
    byte[] decodedContextData = decoded.serviceContexts().get(0).contextData();
    decodedContextData[0] = 0x55;

    assertEquals(List.of(new GiopServiceContext(42, bytes(0x01, 0x02))), decoded.serviceContexts());
  }

  @Test
  void serviceContextValueObjectHasStableEqualityHashAndSummary() {
    GiopServiceContext context = new GiopServiceContext(0xFFFF_FFFFL, bytes(0x01, 0x02));

    assertEquals(context, new GiopServiceContext(0xFFFF_FFFFL, bytes(0x01, 0x02)));
    assertEquals(
        context.hashCode(), new GiopServiceContext(0xFFFF_FFFFL, bytes(0x01, 0x02)).hashCode());
    assertNotEquals(context, new GiopServiceContext(1, bytes(0x01, 0x02)));
    assertNotEquals(context, new GiopServiceContext(0xFFFF_FFFFL, bytes(0x01)));
    assertNotEquals(context, "context");
    assertEquals(
        "GiopServiceContext[contextId=4294967295, contextDataLength=2]", context.toString());
  }

  @Test
  void modelValuesRejectInvalidHeadersUnsignedBoundsAndBlankOperations() {
    assertGiopCode(
        GiopDiagnosticCodes.INVALID_BODY,
        () -> new GiopHeader(GiopVersion.GIOP_1_2, false, false, GiopMessageType.REQUEST, -1));
    assertGiopCode(
        GiopDiagnosticCodes.INVALID_BODY,
        () ->
            new GiopRequest(
                GiopHeader.forType(GiopMessageType.REPLY),
                1,
                3,
                bytes(0x4B),
                "op",
                List.of(),
                bytes()));
    assertGiopCode(
        GiopDiagnosticCodes.INVALID_BODY,
        () ->
            new GiopRequest(
                GiopHeader.forType(GiopMessageType.REQUEST),
                -1,
                3,
                bytes(0x4B),
                "op",
                List.of(),
                bytes()));
    assertGiopCode(
        GiopDiagnosticCodes.INVALID_BODY,
        () ->
            new GiopRequest(
                GiopHeader.forType(GiopMessageType.REQUEST),
                0x1_0000_0000L,
                3,
                bytes(0x4B),
                "op",
                List.of(),
                bytes()));
    assertGiopCode(
        GiopDiagnosticCodes.INVALID_BODY,
        () ->
            new GiopRequest(
                GiopHeader.forType(GiopMessageType.REQUEST),
                1,
                0x100,
                bytes(0x4B),
                "op",
                List.of(),
                bytes()));
    assertGiopCode(
        GiopDiagnosticCodes.INVALID_BODY,
        () ->
            new GiopRequest(
                GiopHeader.forType(GiopMessageType.REQUEST),
                1,
                3,
                bytes(0x4B),
                " ",
                List.of(),
                bytes()));
    assertGiopCode(GiopDiagnosticCodes.INVALID_BODY, () -> new GiopServiceContext(-1, bytes()));
  }

  @Test
  void statusLookupsRejectUnknownWireIdsAndMalformedReservedBytes() {
    assertEquals(GiopReplyStatus.NEEDS_ADDRESSING_MODE, GiopReplyStatus.fromId(5));
    assertEquals(GiopLocateStatus.LOC_NEEDS_ADDRESSING_MODE, GiopLocateStatus.fromId(5));
    assertGiopCode(GiopDiagnosticCodes.INVALID_BODY, () -> GiopReplyStatus.fromId(6));
    assertGiopCode(GiopDiagnosticCodes.INVALID_BODY, () -> GiopLocateStatus.fromId(6));

    byte[] request =
        writer.write(
            new GiopRequest(
                GiopHeader.forType(GiopMessageType.REQUEST),
                1,
                3,
                bytes(0x4B),
                "op",
                List.of(),
                bytes()));
    request[17] = 0x01;

    assertGiopCode(GiopDiagnosticCodes.INVALID_BODY, () -> reader.read(request));
  }

  @Test
  void rejectsMalformedHeadersAndBoundViolations() {
    assertGiopCode(GiopDiagnosticCodes.TRUNCATED_MESSAGE, () -> reader.read(bytes(0x47, 0x49)));
    assertGiopCode(
        GiopDiagnosticCodes.INVALID_MAGIC,
        () -> reader.read(bytes(0x42, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x05, 0, 0, 0, 0)));
    assertGiopCode(
        GiopDiagnosticCodes.UNSUPPORTED_VERSION,
        () -> reader.read(bytes(0x47, 0x49, 0x4F, 0x50, 0x01, 0x01, 0x00, 0x05, 0, 0, 0, 0)));
    assertGiopCode(
        GiopDiagnosticCodes.UNKNOWN_MESSAGE_TYPE,
        () -> reader.read(bytes(0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x08, 0, 0, 0, 0)));
    assertGiopCode(
        GiopDiagnosticCodes.INVALID_FLAGS,
        () -> reader.read(bytes(0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x04, 0x05, 0, 0, 0, 0)));
    assertGiopCode(
        GiopDiagnosticCodes.TRUNCATED_MESSAGE,
        () -> reader.read(bytes(0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x05, 0, 0, 0, 1)));
    assertGiopCode(
        GiopDiagnosticCodes.MESSAGE_SIZE_MISMATCH,
        () -> reader.read(bytes(0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x05, 0, 0, 0, 0, 0)));

    GiopLimits smallMessageLimit =
        new GiopLimits(
            new BoundedLimit("test-message", 11),
            new BoundedLimit("test-body", 100),
            new BoundedLimit("test-context-count", 10),
            new BoundedLimit("test-context-data", 100));
    assertGiopCode(
        GiopDiagnosticCodes.LIMIT_EXCEEDED,
        () ->
            new GiopMessageReader(smallMessageLimit)
                .read(
                    writer.write(
                        new GiopCloseConnection(
                            GiopHeader.forType(GiopMessageType.CLOSE_CONNECTION)))));
  }

  @Test
  void rejectsOversizedServiceContextsUnsupportedTargetsAndTrailingTypedBytes() {
    GiopReply reply =
        new GiopReply(
            GiopHeader.forType(GiopMessageType.REPLY),
            1,
            GiopReplyStatus.NO_EXCEPTION,
            List.of(new GiopServiceContext(1, bytes(0x01, 0x02))),
            bytes());
    byte[] replyBytes = writer.write(reply);
    GiopLimits contextCountLimit =
        new GiopLimits(
            new BoundedLimit("test-message", 1_000),
            new BoundedLimit("test-body", 1_000),
            new BoundedLimit("test-context-count", 0),
            new BoundedLimit("test-context-data", 100));
    GiopLimits contextDataLimit =
        new GiopLimits(
            new BoundedLimit("test-message", 1_000),
            new BoundedLimit("test-body", 1_000),
            new BoundedLimit("test-context-count", 10),
            new BoundedLimit("test-context-data", 1));

    assertGiopCode(
        GiopDiagnosticCodes.LIMIT_EXCEEDED,
        () -> new GiopMessageReader(contextCountLimit).read(replyBytes));
    assertGiopCode(
        GiopDiagnosticCodes.LIMIT_EXCEEDED,
        () -> new GiopMessageReader(contextDataLimit).read(replyBytes));

    byte[] unsupportedTarget =
        writer.write(
            new GiopRequest(
                GiopHeader.forType(GiopMessageType.REQUEST),
                1,
                3,
                bytes(0x4B),
                "a",
                List.of(),
                bytes()));
    unsupportedTarget[21] = 0x7F;
    assertGiopCode(GiopDiagnosticCodes.UNSUPPORTED_BODY, () -> reader.read(unsupportedTarget));

    assertGiopCode(
        GiopDiagnosticCodes.INVALID_BODY,
        () ->
            reader.read(
                bytes(
                    0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x02, 0x00, 0x00, 0x00, 0x05, 0x00,
                    0x00, 0x00, 0x05, 0xAA)));
  }

  @Test
  void giopCodecDoesNotExposeNetworkOrbPoaOrIiopTypes() {
    List<Class<?>> publicTypes =
        List.of(
            GiopMessageReader.class,
            GiopMessageWriter.class,
            GiopRequest.class,
            GiopReply.class,
            GiopCancelRequest.class,
            GiopLocateRequest.class,
            GiopLocateReply.class,
            GiopCloseConnection.class,
            GiopMessageError.class,
            GiopFragment.class);

    for (Class<?> type : publicTypes) {
      for (Field field : type.getDeclaredFields()) {
        String fieldTypeName = field.getType().getName();
        assertTrue(
            fieldTypeName.startsWith("io.github.mundanej.mjo.giop")
                || fieldTypeName.startsWith("io.github.mundanej.mjo.cdr")
                || fieldTypeName.startsWith("io.github.mundanej.mjo.common")
                || fieldTypeName.startsWith("java.")
                || field.getType().isPrimitive()
                || field.getType().isArray(),
            () -> type.getSimpleName() + " exposes unsupported dependency " + fieldTypeName);
      }
    }
  }

  @Test
  @Tag("security")
  void hostileDeclaredBodiesAndContextCountsFailBeforeAllocation() {
    GiopLimits zeroBodyLimit =
        new GiopLimits(
            new BoundedLimit("test-message", 64),
            new BoundedLimit("test-body", 0),
            new BoundedLimit("test-context-count", 1),
            new BoundedLimit("test-context-data", 1));
    assertGiopCode(
        GiopDiagnosticCodes.LIMIT_EXCEEDED,
        () ->
            new GiopMessageReader(zeroBodyLimit)
                .read(
                    bytes(
                        0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x07, 0x00, 0x00, 0x00, 0x01,
                        0x00)));

    GiopLimits oneContextLimit =
        new GiopLimits(
            new BoundedLimit("test-message", 64),
            new BoundedLimit("test-body", 64),
            new BoundedLimit("test-context-count", 1),
            new BoundedLimit("test-context-data", 1));
    assertGiopCode(
        GiopDiagnosticCodes.LIMIT_EXCEEDED,
        () ->
            new GiopMessageReader(oneContextLimit)
                .read(
                    bytes(
                        0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x01, 0x00, 0x00, 0x00, 0x0C,
                        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02)));
  }

  @Test
  @Tag("security")
  void boundedMessageSmokeRemainsDeterministicAcrossRepeatedReads() {
    GiopRequest request =
        new GiopRequest(
            GiopHeader.forType(GiopMessageType.REQUEST),
            91,
            3,
            bytes(0x4B, 0x32),
            "ping",
            List.of(new GiopServiceContext(7, bytes(0x01))),
            bytes(0xCA, 0xFE));
    byte[] encoded = writer.write(request);

    for (int iteration = 0; iteration < 128; iteration++) {
      GiopRequest decoded = assertInstanceOf(GiopRequest.class, reader.read(encoded));
      assertEquals(91, decoded.requestId());
      assertArrayEquals(bytes(0x4B, 0x32), decoded.objectKey());
      assertEquals("ping", decoded.operation());
      assertEquals(List.of(new GiopServiceContext(7, bytes(0x01))), decoded.serviceContexts());
      assertArrayEquals(bytes(0xCA, 0xFE), decoded.body());
    }
  }

  private static void assertGiopCode(Object expectedCode, ThrowingRunnable runnable) {
    GiopException exception = assertThrows(GiopException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int index = 0; index < values.length; index++) {
      bytes[index] = (byte) values[index];
    }
    return bytes;
  }

  private static int indexOf(byte[] haystack, byte[] needle) {
    for (int start = 0; start <= haystack.length - needle.length; start++) {
      boolean matches = true;
      for (int index = 0; index < needle.length; index++) {
        if (haystack[start + index] != needle[index]) {
          matches = false;
          break;
        }
      }
      if (matches) {
        return start;
      }
    }
    throw new AssertionError("needle not found");
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run();
  }
}
