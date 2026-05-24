package io.github.mundanej.mjo.any;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.IiopVersion;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.ObjectKey;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.WireTypeCode;
import io.github.mundanej.mjo.typecode.WireTypeCodeKind;
import io.github.mundanej.mjo.typecode.WireTypeCodeMember;
import io.github.mundanej.mjo.typecode.WireTypeCodeUnionMember;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for wire Any values with embedded wire TypeCodes. */
@Tag("unit")
final class AnyWireCodecTest {

  private final AnyWireCodec codec = new AnyWireCodec();

  @Test
  void wireAnyRoundTripsScalarAliasesStringsEnumsAndDefaults() {
    WireTypeCode longType = WireTypeCode.primitive(WireTypeCodeKind.LONG);
    WireTypeCode enumType =
        WireTypeCode.enumeration("IDL:demo/Color:1.0", "Color", List.of("RED", "BLUE"));
    WireTypeCode aliasType = WireTypeCode.alias("IDL:demo/Count:1.0", "Count", longType);
    WireTypeCode unionType =
        WireTypeCode.union(
            "IDL:demo/Choice:1.0",
            "Choice",
            longType,
            List.of(WireTypeCodeUnionMember.defaultMember("fallback", WireTypeCode.string(0))));

    List<AnyWireValue> values =
        List.of(
            new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.BOOLEAN), true),
            new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.OCTET), 255),
            new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.CHAR), 'A'),
            new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.SHORT), (short) -2),
            new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.UNSIGNED_SHORT), 65535),
            new AnyWireValue(longType, -3),
            new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.UNSIGNED_LONG), 4L),
            new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.LONG_LONG), -5L),
            new AnyWireValue(
                WireTypeCode.primitive(WireTypeCodeKind.UNSIGNED_LONG_LONG), BigInteger.TEN),
            new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.FLOAT), 1.25F),
            new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.DOUBLE), 2.5D),
            new AnyWireValue(WireTypeCode.string(0), "narrow"),
            new AnyWireValue(WireTypeCode.wstring(32), "wide"),
            new AnyWireValue(enumType, "BLUE"),
            new AnyWireValue(aliasType, 42),
            new AnyWireValue(
                unionType,
                new AnyWireUnionValue(99, new AnyWireValue(WireTypeCode.string(0), "x"))));

    for (AnyWireValue value : values) {
      assertEquals(value, roundTrip(value));
    }
    AnyWireValue longDouble =
        roundTrip(
            new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.LONG_DOUBLE), new byte[16]));
    assertEquals(WireTypeCode.primitive(WireTypeCodeKind.LONG_DOUBLE), longDouble.typeCode());
    assertEquals(16, ((byte[]) longDouble.value()).length);
  }

  @Test
  void wireAnyRoundTripsObjectReferencesAggregatesSequencesArraysAndUnions() {
    WireTypeCode longType = WireTypeCode.primitive(WireTypeCodeKind.LONG);
    WireTypeCode objectType = WireTypeCode.objectReference("IDL:demo/Service:1.0", "Service");
    WireTypeCode structType =
        WireTypeCode.struct(
            "IDL:demo/Point:1.0", "Point", List.of(new WireTypeCodeMember("x", longType)));
    WireTypeCode sequenceType = WireTypeCode.sequence(longType, 0);
    WireTypeCode arrayType = WireTypeCode.array(longType, 2);
    WireTypeCode unionType =
        WireTypeCode.union(
            "IDL:demo/Choice:1.0",
            "Choice",
            longType,
            List.of(WireTypeCodeUnionMember.label("x", 1, longType)));
    Ior ior =
        new Ior(
            "IDL:demo/Service:1.0",
            List.of(
                TaggedProfile.internetIop(
                    new IiopProfile(
                        IiopVersion.V1_0, "host", 9, new ObjectKey(new byte[] {1}), List.of()))));

    assertEquals(new AnyWireValue(objectType, ior), roundTrip(new AnyWireValue(objectType, ior)));
    assertEquals(
        new AnyWireValue(structType, List.of(new AnyWireValue(longType, 7))),
        roundTrip(new AnyWireValue(structType, List.of(new AnyWireValue(longType, 7)))));
    assertEquals(
        new AnyWireValue(sequenceType, List.of(new AnyWireValue(longType, 1))),
        roundTrip(new AnyWireValue(sequenceType, List.of(new AnyWireValue(longType, 1)))));
    assertEquals(
        new AnyWireValue(
            arrayType, List.of(new AnyWireValue(longType, 1), new AnyWireValue(longType, 2))),
        roundTrip(
            new AnyWireValue(
                arrayType, List.of(new AnyWireValue(longType, 1), new AnyWireValue(longType, 2)))));
    assertEquals(
        new AnyWireValue(unionType, new AnyWireUnionValue(1, new AnyWireValue(longType, 12))),
        roundTrip(
            new AnyWireValue(unionType, new AnyWireUnionValue(1, new AnyWireValue(longType, 12)))));
  }

  @Test
  void localObjectReferenceCodecRoundTripsIors() {
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
    Ior ior =
        new Ior(
            "IDL:demo/Service:1.0",
            List.of(
                TaggedProfile.internetIop(
                    new IiopProfile(
                        IiopVersion.V1_2,
                        "127.0.0.1",
                        2099,
                        new ObjectKey(new byte[] {4, 5}),
                        List.of()))));
    AnyValueCodec<Ior> objectCodec = AnyCodecs.objectReference(serviceType);
    CdrWriter writer = new CdrWriter(CdrByteOrder.BIG_ENDIAN);

    objectCodec.write(writer, ior);

    assertEquals(
        ior, objectCodec.read(new CdrReader(CdrByteOrder.BIG_ENDIAN, writer.toByteArray())));
    assertEquals(
        serviceType,
        objectCodec
            .readAny(new CdrReader(CdrByteOrder.BIG_ENDIAN, writer.toByteArray()))
            .typeCode());
    assertThrows(AnyException.class, () -> AnyCodecs.objectReference(IdlTypeCode.LONG));
  }

  @Test
  void wireAnyRejectsMismatchedValuesDeterministically() {
    WireTypeCode longType = WireTypeCode.primitive(WireTypeCodeKind.LONG);
    WireTypeCode arrayType = WireTypeCode.array(longType, 2);

    assertThrows(
        AnyException.class,
        () -> write(new AnyWireValue(arrayType, List.of(new AnyWireValue(longType, 1)))));
    assertThrows(
        ClassCastException.class,
        () -> write(new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.BOOLEAN), "true")));
    assertThrows(
        AnyException.class,
        () ->
            write(
                new AnyWireValue(
                    WireTypeCode.sequence(longType, 0),
                    List.of(new AnyWireValue(WireTypeCode.string(0), "wrong")))));
    assertThrows(
        AnyException.class,
        () ->
            write(
                new AnyWireValue(
                    WireTypeCode.union(
                        "IDL:demo/Choice:1.0",
                        "Choice",
                        longType,
                        List.of(WireTypeCodeUnionMember.label("x", 1, longType))),
                    new AnyWireUnionValue(2, new AnyWireValue(longType, 1)))));
    assertThrows(
        AnyException.class,
        () -> write(new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.VOID), "none")));
  }

  private AnyWireValue roundTrip(AnyWireValue value) {
    byte[] encoded = write(value);
    return codec.read(new CdrReader(CdrByteOrder.BIG_ENDIAN, encoded));
  }

  private byte[] write(AnyWireValue value) {
    CdrWriter writer = new CdrWriter(CdrByteOrder.BIG_ENDIAN);
    codec.write(writer, value);
    return writer.toByteArray();
  }
}
