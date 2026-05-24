package io.github.mundanej.mjo.typecode;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/** Deterministic recursive CDR codec for the supported wire TypeCode subset. */
public final class WireTypeCodeCodec {

  private static final int MAX_DEPTH = 64;

  /** Reads one wire TypeCode. */
  public WireTypeCode read(CdrReader reader) {
    return read(reader, 0);
  }

  /** Writes one wire TypeCode. */
  public void write(CdrWriter writer, WireTypeCode typeCode) {
    write(writer, typeCode, 0);
  }

  private WireTypeCode read(CdrReader reader, int depth) {
    requireDepth(depth);
    WireTypeCodeKind kind = WireTypeCodeKind.fromId(reader.readUnsignedLong());
    return switch (kind) {
      case STRING -> WireTypeCode.string(Math.toIntExact(reader.readUnsignedLong()));
      case WSTRING -> WireTypeCode.wstring(Math.toIntExact(reader.readUnsignedLong()));
      case OBJECT_REFERENCE ->
          WireTypeCode.objectReference(reader.readString(), reader.readString());
      case STRUCT -> readStruct(reader, depth);
      case EXCEPTION -> readException(reader, depth);
      case ENUM -> readEnum(reader);
      case ALIAS ->
          WireTypeCode.alias(reader.readString(), reader.readString(), read(reader, depth + 1));
      case SEQUENCE ->
          WireTypeCode.sequence(
              read(reader, depth + 1), Math.toIntExact(reader.readUnsignedLong()));
      case ARRAY ->
          WireTypeCode.array(read(reader, depth + 1), Math.toIntExact(reader.readUnsignedLong()));
      case UNION -> readUnion(reader, depth);
      default -> WireTypeCode.primitive(kind);
    };
  }

  private void write(CdrWriter writer, WireTypeCode typeCode, int depth) {
    requireDepth(depth);
    writer.writeUnsignedLong(typeCode.kind().id());
    switch (typeCode.kind()) {
      case STRING, WSTRING -> writer.writeUnsignedLong(typeCode.bound().orElseThrow());
      case OBJECT_REFERENCE -> writeNamed(writer, typeCode);
      case STRUCT, EXCEPTION -> writeMembers(writer, typeCode, depth);
      case ENUM -> writeEnum(writer, typeCode);
      case ALIAS -> writeAlias(writer, typeCode, depth);
      case SEQUENCE, ARRAY -> writeContentAndBound(writer, typeCode, depth);
      case UNION -> writeUnion(writer, typeCode, depth);
      default -> {
        // Scalar TypeCodes have no parameters in this compact wire helper.
      }
    }
  }

  private WireTypeCode readStruct(CdrReader reader, int depth) {
    return WireTypeCode.struct(
        reader.readString(), reader.readString(), readMembers(reader, depth));
  }

  private WireTypeCode readException(CdrReader reader, int depth) {
    return WireTypeCode.exception(
        reader.readString(), reader.readString(), readMembers(reader, depth));
  }

  private WireTypeCode readEnum(CdrReader reader) {
    String repositoryId = reader.readString();
    String name = reader.readString();
    int count = reader.readSequenceLength();
    List<String> constants = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      constants.add(reader.readString());
    }
    return WireTypeCode.enumeration(repositoryId, name, constants);
  }

  private WireTypeCode readUnion(CdrReader reader, int depth) {
    String repositoryId = reader.readString();
    String name = reader.readString();
    WireTypeCode discriminator = read(reader, depth + 1);
    int count = reader.readSequenceLength();
    List<WireTypeCodeUnionMember> members = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      boolean defaultMember = reader.readBoolean();
      long label = reader.readUnsignedLong();
      String memberName = reader.readString();
      WireTypeCode memberType = read(reader, depth + 1);
      OptionalLong optionalLabel = defaultMember ? OptionalLong.empty() : OptionalLong.of(label);
      members.add(new WireTypeCodeUnionMember(memberName, optionalLabel, memberType));
    }
    return WireTypeCode.union(repositoryId, name, discriminator, members);
  }

  private List<WireTypeCodeMember> readMembers(CdrReader reader, int depth) {
    int count = reader.readSequenceLength();
    List<WireTypeCodeMember> members = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      members.add(new WireTypeCodeMember(reader.readString(), read(reader, depth + 1)));
    }
    return members;
  }

  private void writeNamed(CdrWriter writer, WireTypeCode typeCode) {
    writer.writeString(typeCode.repositoryId().orElseThrow());
    writer.writeString(typeCode.name().orElseThrow());
  }

  private void writeMembers(CdrWriter writer, WireTypeCode typeCode, int depth) {
    writeNamed(writer, typeCode);
    writer.writeSequenceLength(typeCode.members().size());
    for (WireTypeCodeMember member : typeCode.members()) {
      writer.writeString(member.name());
      write(writer, member.typeCode(), depth + 1);
    }
  }

  private void writeEnum(CdrWriter writer, WireTypeCode typeCode) {
    writeNamed(writer, typeCode);
    writer.writeSequenceLength(typeCode.enumConstants().size());
    for (String constant : typeCode.enumConstants()) {
      writer.writeString(constant);
    }
  }

  private void writeAlias(CdrWriter writer, WireTypeCode typeCode, int depth) {
    writeNamed(writer, typeCode);
    write(writer, typeCode.contentType().orElseThrow(), depth + 1);
  }

  private void writeContentAndBound(CdrWriter writer, WireTypeCode typeCode, int depth) {
    write(writer, typeCode.contentType().orElseThrow(), depth + 1);
    writer.writeUnsignedLong(typeCode.bound().orElseThrow());
  }

  private void writeUnion(CdrWriter writer, WireTypeCode typeCode, int depth) {
    writeNamed(writer, typeCode);
    write(writer, typeCode.discriminatorType().orElseThrow(), depth + 1);
    writer.writeSequenceLength(typeCode.unionMembers().size());
    for (WireTypeCodeUnionMember member : typeCode.unionMembers()) {
      writer.writeBoolean(member.label().isEmpty());
      writer.writeUnsignedLong(member.label().orElse(0));
      writer.writeString(member.name());
      write(writer, member.typeCode(), depth + 1);
    }
  }

  private static void requireDepth(int depth) {
    if (depth > MAX_DEPTH) {
      throw new IllegalArgumentException("wire TypeCode nesting exceeds " + MAX_DEPTH);
    }
  }
}
