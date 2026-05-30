package io.github.mundanej.mjo.typecode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for G10 wire TypeCode metadata encoding. */
@Tag("unit")
final class WireTypeCodeCodecTest {

  private final WireTypeCodeCodec codec = new WireTypeCodeCodec();

  @Test
  void wireTypeCodesRoundTripSupportedPeerShapes() {
    WireTypeCode longType = WireTypeCode.primitive(WireTypeCodeKind.LONG);
    WireTypeCode struct =
        WireTypeCode.struct(
            "IDL:demo/Point:1.0",
            "Point",
            List.of(new WireTypeCodeMember("x", longType), new WireTypeCodeMember("y", longType)));
    WireTypeCode union =
        WireTypeCode.union(
            "IDL:demo/Choice:1.0",
            "Choice",
            longType,
            List.of(
                WireTypeCodeUnionMember.label("number", 1, longType),
                WireTypeCodeUnionMember.label("max", 0xFFFF_FFFFL, longType),
                WireTypeCodeUnionMember.defaultMember("name", WireTypeCode.string(0))));
    List<WireTypeCode> values =
        List.of(
            WireTypeCode.wstring(64),
            WireTypeCode.objectReference("IDL:demo/Service:1.0", "Service"),
            struct,
            WireTypeCode.exception("IDL:demo/Problem:1.0", "Problem", struct.members()),
            WireTypeCode.enumeration("IDL:demo/Color:1.0", "Color", List.of("RED", "BLUE")),
            WireTypeCode.alias("IDL:demo/Alias:1.0", "Alias", struct),
            WireTypeCode.sequence(struct, 10),
            WireTypeCode.array(longType, 3),
            union);

    for (WireTypeCode value : values) {
      assertEquals(value, roundTrip(value));
    }
  }

  @Test
  void localTypeCodesMapToWireShapesWithoutChangingLocalModel() {
    IdlTypeCode localInterface =
        IdlTypeCode.fromTypeReference(
            new IdlTypeReference(
                IdlTypeKind.INTERFACE,
                "::demo::Service",
                "demo.Service",
                Optional.of(RepositoryId.parse("IDL:demo/Service:1.0"))));

    WireTypeCode wire = WireTypeCodes.fromLocal(localInterface);

    assertEquals(WireTypeCodeKind.OBJECT_REFERENCE, wire.kind());
    assertEquals(Optional.of("IDL:demo/Service:1.0"), wire.repositoryId());
    assertEquals(Optional.of("::demo::Service"), wire.name());
  }

  @Test
  void g12DeferredLocalTypeCodesFailWithStableWireMappingMessage() {
    IdlTypeCode nativeType =
        IdlTypeCode.fromTypeReference(
            new IdlTypeReference(
                IdlTypeKind.NATIVE,
                "::demo::Handle",
                "demo.Handle",
                Optional.of(RepositoryId.parse("IDL:demo/Handle:1.0"))));
    IdlTypeCode valueBox =
        IdlTypeCode.fromDescriptor(
            new IdlGeneratedTypeDescriptor(
                IdlTypeKind.VALUE_BOX,
                "::demo::Name",
                "demo.Name",
                RepositoryId.parse("IDL:demo/Name:1.0"),
                List.of(
                    new IdlFieldDescriptor(
                        "value",
                        new IdlTypeReference(
                            IdlTypeKind.PRIMITIVE,
                            "string",
                            "java.lang.String",
                            Optional.empty()))),
                List.of(),
                List.of()));

    IllegalArgumentException nativeFailure =
        assertThrows(IllegalArgumentException.class, () -> WireTypeCodes.fromLocal(nativeType));
    IllegalArgumentException valueBoxFailure =
        assertThrows(IllegalArgumentException.class, () -> WireTypeCodes.fromLocal(valueBox));

    assertEquals("wire TypeCode mapping deferred for NATIVE", nativeFailure.getMessage());
    assertEquals("wire TypeCode mapping deferred for VALUE_BOX", valueBoxFailure.getMessage());
  }

  @Test
  void malformedWireTypeCodesFailDeterministically() {
    assertThrows(
        IllegalArgumentException.class, () -> WireTypeCode.array(WireTypeCode.string(0), 0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            codec.read(
                CdrReader.bigEndian(
                    new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF})));
  }

  private WireTypeCode roundTrip(WireTypeCode typeCode) {
    CdrWriter writer = new CdrWriter(CdrByteOrder.BIG_ENDIAN);
    codec.write(writer, typeCode);
    return codec.read(new CdrReader(CdrByteOrder.BIG_ENDIAN, writer.toByteArray()));
  }
}
