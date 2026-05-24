package io.github.mundanej.mjo.any;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.typecode.WireTypeCode;
import io.github.mundanej.mjo.typecode.WireTypeCodeCodec;
import io.github.mundanej.mjo.typecode.WireTypeCodeMember;
import io.github.mundanej.mjo.typecode.WireTypeCodeUnionMember;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** CDR codec for wire Any values in the G10 TypeCode subset. */
public final class AnyWireCodec {

  private final WireTypeCodeCodec typeCodeCodec = new WireTypeCodeCodec();

  /** Reads a TypeCode followed by its matching value body. */
  public AnyWireValue read(CdrReader reader) {
    WireTypeCode typeCode = typeCodeCodec.read(reader);
    return new AnyWireValue(typeCode, readValue(reader, typeCode));
  }

  /** Writes a TypeCode followed by its matching value body. */
  public void write(CdrWriter writer, AnyWireValue value) {
    Objects.requireNonNull(value, "value");
    typeCodeCodec.write(writer, value.typeCode());
    writeValue(writer, value.typeCode(), value.value());
  }

  private Object readValue(CdrReader reader, WireTypeCode typeCode) {
    return switch (typeCode.kind()) {
      case BOOLEAN -> reader.readBoolean();
      case OCTET -> reader.readOctet();
      case CHAR -> reader.readChar();
      case SHORT -> reader.readShort();
      case UNSIGNED_SHORT -> reader.readUnsignedShort();
      case LONG -> reader.readLong();
      case UNSIGNED_LONG -> reader.readUnsignedLong();
      case LONG_LONG -> reader.readLongLong();
      case UNSIGNED_LONG_LONG -> reader.readUnsignedLongLong();
      case FLOAT -> reader.readFloat();
      case DOUBLE -> reader.readDouble();
      case LONG_DOUBLE -> reader.readLongDoubleBytes();
      case STRING -> reader.readString();
      case WSTRING -> reader.readWString();
      case OBJECT_REFERENCE ->
          Ior.readFrom(reader, io.github.mundanej.mjo.ior.IorLimits.defaults());
      case STRUCT, EXCEPTION -> readAggregate(reader, typeCode);
      case ENUM -> readEnum(reader, typeCode);
      case ALIAS -> readValue(reader, typeCode.contentType().orElseThrow());
      case SEQUENCE -> readSequence(reader, typeCode.contentType().orElseThrow());
      case ARRAY ->
          readArray(reader, typeCode.contentType().orElseThrow(), typeCode.bound().orElseThrow());
      case UNION -> readUnion(reader, typeCode);
      default -> throw unsupported(typeCode);
    };
  }

  private void writeValue(CdrWriter writer, WireTypeCode typeCode, Object value) {
    Objects.requireNonNull(value, "value");
    switch (typeCode.kind()) {
      case BOOLEAN -> writer.writeBoolean((Boolean) value);
      case OCTET -> writer.writeOctet((Integer) value);
      case CHAR -> writer.writeChar((Character) value);
      case SHORT -> writer.writeShort((Short) value);
      case UNSIGNED_SHORT -> writer.writeUnsignedShort((Integer) value);
      case LONG -> writer.writeLong((Integer) value);
      case UNSIGNED_LONG -> writer.writeUnsignedLong((Long) value);
      case LONG_LONG -> writer.writeLongLong((Long) value);
      case UNSIGNED_LONG_LONG -> writer.writeUnsignedLongLong((BigInteger) value);
      case FLOAT -> writer.writeFloat((Float) value);
      case DOUBLE -> writer.writeDouble((Double) value);
      case LONG_DOUBLE -> writer.writeLongDoubleBytes((byte[]) value);
      case STRING -> writer.writeString((String) value);
      case WSTRING -> writer.writeWString((String) value);
      case OBJECT_REFERENCE -> ((Ior) value).writeTo(writer);
      case STRUCT, EXCEPTION -> writeAggregate(writer, typeCode, listValue(value));
      case ENUM -> writeEnum(writer, typeCode, (String) value);
      case ALIAS -> writeValue(writer, typeCode.contentType().orElseThrow(), value);
      case SEQUENCE ->
          writeSequence(writer, typeCode.contentType().orElseThrow(), listValue(value));
      case ARRAY -> writeArray(writer, typeCode, listValue(value));
      case UNION -> writeUnion(writer, typeCode, (AnyWireUnionValue) value);
      default -> throw unsupported(typeCode);
    }
  }

  private List<AnyWireValue> readAggregate(CdrReader reader, WireTypeCode typeCode) {
    List<AnyWireValue> values = new ArrayList<>(typeCode.members().size());
    for (WireTypeCodeMember member : typeCode.members()) {
      values.add(new AnyWireValue(member.typeCode(), readValue(reader, member.typeCode())));
    }
    return List.copyOf(values);
  }

  private void writeAggregate(CdrWriter writer, WireTypeCode typeCode, List<AnyWireValue> values) {
    if (values.size() != typeCode.members().size()) {
      throw new AnyException(AnyDiagnosticCodes.TYPE_MISMATCH, "aggregate member count mismatch");
    }
    for (int index = 0; index < values.size(); index++) {
      WireTypeCode memberType = typeCode.members().get(index).typeCode();
      AnyWireValue memberValue = values.get(index);
      if (!memberType.equals(memberValue.typeCode())) {
        throw new AnyException(
            AnyDiagnosticCodes.TYPE_MISMATCH, "aggregate member TypeCode mismatch");
      }
      writeValue(writer, memberType, memberValue.value());
    }
  }

  private String readEnum(CdrReader reader, WireTypeCode typeCode) {
    long ordinal = reader.readUnsignedLong();
    if (ordinal >= typeCode.enumConstants().size()) {
      throw new AnyException(AnyDiagnosticCodes.INVALID_ENUM_VALUE, "enum ordinal out of range");
    }
    return typeCode.enumConstants().get(Math.toIntExact(ordinal));
  }

  private void writeEnum(CdrWriter writer, WireTypeCode typeCode, String value) {
    int ordinal = typeCode.enumConstants().indexOf(value);
    if (ordinal < 0) {
      throw new AnyException(AnyDiagnosticCodes.INVALID_ENUM_VALUE, "unknown enum label: " + value);
    }
    writer.writeUnsignedLong(ordinal);
  }

  private List<AnyWireValue> readSequence(CdrReader reader, WireTypeCode elementType) {
    int count = reader.readSequenceLength();
    return readArray(reader, elementType, count);
  }

  private List<AnyWireValue> readArray(CdrReader reader, WireTypeCode elementType, int count) {
    List<AnyWireValue> values = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      values.add(new AnyWireValue(elementType, readValue(reader, elementType)));
    }
    return List.copyOf(values);
  }

  private void writeSequence(
      CdrWriter writer, WireTypeCode elementType, List<AnyWireValue> values) {
    writer.writeSequenceLength(values.size());
    for (AnyWireValue value : values) {
      requireElementType(elementType, value);
      writeValue(writer, elementType, value.value());
    }
  }

  private void writeArray(CdrWriter writer, WireTypeCode typeCode, List<AnyWireValue> values) {
    int expected = typeCode.bound().orElseThrow();
    if (values.size() != expected) {
      throw new AnyException(AnyDiagnosticCodes.TYPE_MISMATCH, "array element count mismatch");
    }
    WireTypeCode elementType = typeCode.contentType().orElseThrow();
    for (AnyWireValue value : values) {
      requireElementType(elementType, value);
      writeValue(writer, elementType, value.value());
    }
  }

  private AnyWireUnionValue readUnion(CdrReader reader, WireTypeCode typeCode) {
    long label = reader.readUnsignedLong();
    WireTypeCodeUnionMember member = selectUnionMember(typeCode, label);
    return new AnyWireUnionValue(
        label, new AnyWireValue(member.typeCode(), readValue(reader, member.typeCode())));
  }

  private void writeUnion(CdrWriter writer, WireTypeCode typeCode, AnyWireUnionValue value) {
    WireTypeCodeUnionMember member = selectUnionMember(typeCode, value.label());
    requireElementType(member.typeCode(), value.value());
    writer.writeUnsignedLong(value.label());
    writeValue(writer, member.typeCode(), value.value().value());
  }

  private static WireTypeCodeUnionMember selectUnionMember(WireTypeCode typeCode, long label) {
    WireTypeCodeUnionMember defaultMember = null;
    for (WireTypeCodeUnionMember member : typeCode.unionMembers()) {
      if (member.label().isPresent() && member.label().orElseThrow() == label) {
        return member;
      }
      if (member.label().isEmpty()) {
        defaultMember = member;
      }
    }
    if (defaultMember != null) {
      return defaultMember;
    }
    throw new AnyException(AnyDiagnosticCodes.TYPE_MISMATCH, "unknown union label: " + label);
  }

  @SuppressWarnings("unchecked")
  private static List<AnyWireValue> listValue(Object value) {
    return List.copyOf((List<AnyWireValue>) value);
  }

  private static void requireElementType(WireTypeCode elementType, AnyWireValue value) {
    if (!elementType.equals(value.typeCode())) {
      throw new AnyException(AnyDiagnosticCodes.TYPE_MISMATCH, "element TypeCode mismatch");
    }
  }

  private static AnyException unsupported(WireTypeCode typeCode) {
    return new AnyException(
        AnyDiagnosticCodes.UNSUPPORTED_TYPE, "unsupported wire Any TypeCode: " + typeCode.kind());
  }
}
