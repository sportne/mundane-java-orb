package io.github.mundanej.mjo.any;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import io.github.mundanej.mjo.typecode.IdlTypeCodeKind;
import io.github.mundanej.mjo.typecode.IdlTypeCodeMember;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Static local Any CDR payload codecs for the supported TypeCode slice. */
public final class AnyCodecs {

  private AnyCodecs() {}

  public static AnyValueCodec<Boolean> booleanCodec() {
    return scalar(IdlTypeCode.BOOLEAN, CdrReader::readBoolean, CdrWriter::writeBoolean);
  }

  public static AnyValueCodec<Integer> octetCodec() {
    return scalar(IdlTypeCode.OCTET, CdrReader::readOctet, CdrWriter::writeOctet);
  }

  public static AnyValueCodec<Character> charCodec() {
    return scalar(IdlTypeCode.CHAR, CdrReader::readChar, CdrWriter::writeChar);
  }

  public static AnyValueCodec<Short> shortCodec() {
    return scalar(IdlTypeCode.SHORT, CdrReader::readShort, CdrWriter::writeShort);
  }

  public static AnyValueCodec<Integer> unsignedShortCodec() {
    return scalar(
        IdlTypeCode.UNSIGNED_SHORT, CdrReader::readUnsignedShort, CdrWriter::writeUnsignedShort);
  }

  public static AnyValueCodec<Integer> longCodec() {
    return scalar(IdlTypeCode.LONG, CdrReader::readLong, CdrWriter::writeLong);
  }

  public static AnyValueCodec<Long> unsignedLongCodec() {
    return scalar(
        IdlTypeCode.UNSIGNED_LONG, CdrReader::readUnsignedLong, CdrWriter::writeUnsignedLong);
  }

  public static AnyValueCodec<Long> longLongCodec() {
    return scalar(IdlTypeCode.LONG_LONG, CdrReader::readLongLong, CdrWriter::writeLongLong);
  }

  public static AnyValueCodec<BigInteger> unsignedLongLongCodec() {
    return scalar(
        IdlTypeCode.UNSIGNED_LONG_LONG,
        CdrReader::readUnsignedLongLong,
        CdrWriter::writeUnsignedLongLong);
  }

  public static AnyValueCodec<Float> floatCodec() {
    return scalar(IdlTypeCode.FLOAT, CdrReader::readFloat, CdrWriter::writeFloat);
  }

  public static AnyValueCodec<Double> doubleCodec() {
    return scalar(IdlTypeCode.DOUBLE, CdrReader::readDouble, CdrWriter::writeDouble);
  }

  public static AnyValueCodec<byte[]> longDoubleCodec() {
    return new AnyValueCodec<>() {
      @Override
      public IdlTypeCode typeCode() {
        return IdlTypeCode.LONG_DOUBLE;
      }

      @Override
      public byte[] read(CdrReader reader) {
        return reader.readLongDoubleBytes();
      }

      @Override
      public void write(CdrWriter writer, byte[] value) {
        writer.writeLongDoubleBytes(
            Arrays.copyOf(Objects.requireNonNull(value, "value"), value.length));
      }
    };
  }

  public static AnyValueCodec<String> stringCodec() {
    return scalar(IdlTypeCode.STRING, CdrReader::readString, CdrWriter::writeString);
  }

  /** Creates an object-reference codec backed by the existing IOR wire model. */
  public static AnyValueCodec<Ior> objectReference(IdlTypeCode typeCode) {
    requireKind(typeCode, IdlTypeCodeKind.INTERFACE);
    return new AnyValueCodec<>() {
      @Override
      public IdlTypeCode typeCode() {
        return typeCode;
      }

      @Override
      public Ior read(CdrReader reader) {
        return Ior.readFrom(reader, io.github.mundanej.mjo.ior.IorLimits.defaults());
      }

      @Override
      public void write(CdrWriter writer, Ior value) {
        Objects.requireNonNull(value, "value").writeTo(writer);
      }
    };
  }

  /** Creates an enum codec that maps CDR unsigned-long ordinals to IDL constant names. */
  public static AnyValueCodec<String> enumeration(IdlTypeCode typeCode) {
    requireKind(typeCode, IdlTypeCodeKind.ENUM);
    return new AnyValueCodec<>() {
      @Override
      public IdlTypeCode typeCode() {
        return typeCode;
      }

      @Override
      public String read(CdrReader reader) {
        long ordinal = reader.readUnsignedLong();
        if (ordinal > Integer.MAX_VALUE || ordinal >= typeCode.enumConstants().size()) {
          throw new AnyException(
              AnyDiagnosticCodes.INVALID_ENUM_VALUE, "enum ordinal out of range: " + ordinal);
        }
        return typeCode.enumConstants().get((int) ordinal);
      }

      @Override
      public void write(CdrWriter writer, String value) {
        Objects.requireNonNull(value, "value");
        int ordinal = typeCode.enumConstants().indexOf(value);
        if (ordinal < 0) {
          throw new AnyException(
              AnyDiagnosticCodes.INVALID_ENUM_VALUE, "unknown enum label: " + value);
        }
        writer.writeUnsignedLong(ordinal);
      }
    };
  }

  /** Creates a struct or exception codec using explicit member codecs. */
  public static AnyValueCodec<AnyAggregateValue> aggregate(
      IdlTypeCode typeCode, Map<String, AnyValueCodec<?>> memberCodecs) {
    Objects.requireNonNull(typeCode, "typeCode");
    if (!typeCode.isAggregate()) {
      throw new AnyException(
          AnyDiagnosticCodes.TYPE_MISMATCH,
          "aggregate codec requires a struct or exception TypeCode: " + typeCode.kind());
    }
    Map<String, AnyValueCodec<?>> codecs = Map.copyOf(Objects.requireNonNull(memberCodecs));
    validateAggregateCodecs(typeCode, codecs);
    return new AnyValueCodec<>() {
      @Override
      public IdlTypeCode typeCode() {
        return typeCode;
      }

      @Override
      public AnyAggregateValue read(CdrReader reader) {
        java.util.LinkedHashMap<String, AnyValue<?>> members = new java.util.LinkedHashMap<>();
        for (IdlTypeCodeMember member : typeCode.members()) {
          AnyValueCodec<?> codec = codecs.get(member.name());
          members.put(member.name(), codec.readAny(reader));
        }
        return new AnyAggregateValue(typeCode, members);
      }

      @Override
      public void write(CdrWriter writer, AnyAggregateValue value) {
        Objects.requireNonNull(value, "value");
        if (!typeCode.equals(value.typeCode())) {
          throw new AnyException(
              AnyDiagnosticCodes.TYPE_MISMATCH, "aggregate value TypeCode does not match codec");
        }
        validateAggregateMembers(typeCode, value.members());
        for (IdlTypeCodeMember member : typeCode.members()) {
          writeUntyped(codecs.get(member.name()), writer, value.member(member.name()));
        }
      }
    };
  }

  /** Creates a sequence codec for unbounded local sequences. */
  public static <T> AnyValueCodec<List<T>> sequence(
      IdlTypeCode typeCode, AnyValueCodec<T> elementCodec) {
    requireKind(typeCode, IdlTypeCodeKind.SEQUENCE);
    Objects.requireNonNull(elementCodec, "elementCodec");
    if (!typeCode.elementType().orElseThrow().equals(elementCodec.typeCode())) {
      throw new AnyException(
          AnyDiagnosticCodes.TYPE_MISMATCH, "sequence element TypeCode does not match codec");
    }
    return new AnyValueCodec<>() {
      @Override
      public IdlTypeCode typeCode() {
        return typeCode;
      }

      @Override
      public List<T> read(CdrReader reader) {
        int size = reader.readSequenceLength();
        List<T> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
          result.add(elementCodec.read(reader));
        }
        return List.copyOf(result);
      }

      @Override
      public void write(CdrWriter writer, List<T> value) {
        Objects.requireNonNull(value, "value");
        writer.writeSequenceLength(value.size());
        for (T element : value) {
          elementCodec.write(writer, Objects.requireNonNull(element, "sequence element"));
        }
      }
    };
  }

  private static <T> AnyValueCodec<T> scalar(
      IdlTypeCode typeCode, Reader<T> reader, Writer<T> writer) {
    return new AnyValueCodec<>() {
      @Override
      public IdlTypeCode typeCode() {
        return typeCode;
      }

      @Override
      public T read(CdrReader cdrReader) {
        return reader.read(cdrReader);
      }

      @Override
      public void write(CdrWriter cdrWriter, T value) {
        writer.write(cdrWriter, Objects.requireNonNull(value, "value"));
      }
    };
  }

  private static void validateAggregateCodecs(
      IdlTypeCode typeCode, Map<String, AnyValueCodec<?>> codecs) {
    Set<String> expected = memberNames(typeCode);
    for (String name : codecs.keySet()) {
      if (!expected.contains(name)) {
        throw new AnyException(AnyDiagnosticCodes.UNKNOWN_MEMBER, "unknown member codec: " + name);
      }
    }
    for (IdlTypeCodeMember member : typeCode.members()) {
      AnyValueCodec<?> codec = codecs.get(member.name());
      if (codec == null) {
        throw new AnyException(
            AnyDiagnosticCodes.MISSING_MEMBER, "missing member codec: " + member.name());
      }
      if (!member.type().equals(codec.typeCode())) {
        throw new AnyException(
            AnyDiagnosticCodes.TYPE_MISMATCH, "member codec TypeCode mismatch: " + member.name());
      }
    }
  }

  private static void validateAggregateMembers(
      IdlTypeCode typeCode, Map<String, AnyValue<?>> members) {
    Set<String> expected = memberNames(typeCode);
    for (String name : members.keySet()) {
      if (!expected.contains(name)) {
        throw new AnyException(AnyDiagnosticCodes.UNKNOWN_MEMBER, "unknown member value: " + name);
      }
    }
    for (IdlTypeCodeMember member : typeCode.members()) {
      AnyValue<?> value = members.get(member.name());
      if (value == null) {
        throw new AnyException(
            AnyDiagnosticCodes.MISSING_MEMBER, "missing member value: " + member.name());
      }
      if (!member.type().equals(value.typeCode())) {
        throw new AnyException(
            AnyDiagnosticCodes.TYPE_MISMATCH, "member value TypeCode mismatch: " + member.name());
      }
    }
  }

  private static Set<String> memberNames(IdlTypeCode typeCode) {
    Set<String> names = new LinkedHashSet<>();
    for (IdlTypeCodeMember member : typeCode.members()) {
      names.add(member.name());
    }
    return names;
  }

  private static void requireKind(IdlTypeCode typeCode, IdlTypeCodeKind kind) {
    Objects.requireNonNull(typeCode, "typeCode");
    if (typeCode.kind() != kind) {
      throw new AnyException(
          AnyDiagnosticCodes.TYPE_MISMATCH,
          "TypeCode kind must be " + kind + ": " + typeCode.kind());
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> void writeUntyped(
      AnyValueCodec<T> codec, CdrWriter writer, AnyValue<?> value) {
    codec.writeAny(writer, (AnyValue<T>) value);
  }

  @FunctionalInterface
  private interface Reader<T> {
    T read(CdrReader reader);
  }

  @FunctionalInterface
  private interface Writer<T> {
    void write(CdrWriter writer, T value);
  }
}
