package io.github.mundanej.mjo.typecode;

import java.util.List;

/** Mapping helpers between local descriptor TypeCodes and wire TypeCodes. */
public final class WireTypeCodes {

  private WireTypeCodes() {}

  /** Maps the existing local TypeCode model to the G10 wire TypeCode model. */
  public static WireTypeCode fromLocal(IdlTypeCode typeCode) {
    return switch (typeCode.kind()) {
      case VOID -> WireTypeCode.primitive(WireTypeCodeKind.VOID);
      case BOOLEAN -> WireTypeCode.primitive(WireTypeCodeKind.BOOLEAN);
      case OCTET -> WireTypeCode.primitive(WireTypeCodeKind.OCTET);
      case CHAR -> WireTypeCode.primitive(WireTypeCodeKind.CHAR);
      case SHORT -> WireTypeCode.primitive(WireTypeCodeKind.SHORT);
      case UNSIGNED_SHORT -> WireTypeCode.primitive(WireTypeCodeKind.UNSIGNED_SHORT);
      case LONG -> WireTypeCode.primitive(WireTypeCodeKind.LONG);
      case UNSIGNED_LONG -> WireTypeCode.primitive(WireTypeCodeKind.UNSIGNED_LONG);
      case LONG_LONG -> WireTypeCode.primitive(WireTypeCodeKind.LONG_LONG);
      case UNSIGNED_LONG_LONG -> WireTypeCode.primitive(WireTypeCodeKind.UNSIGNED_LONG_LONG);
      case FLOAT -> WireTypeCode.primitive(WireTypeCodeKind.FLOAT);
      case DOUBLE -> WireTypeCode.primitive(WireTypeCodeKind.DOUBLE);
      case LONG_DOUBLE -> WireTypeCode.primitive(WireTypeCodeKind.LONG_DOUBLE);
      case STRING -> WireTypeCode.string(0);
      case INTERFACE ->
          WireTypeCode.objectReference(
              typeCode.repositoryId().orElseThrow().value(), typeCode.idlName());
      case STRUCT ->
          WireTypeCode.struct(
              typeCode.repositoryId().orElseThrow().value(),
              typeCode.idlName(),
              localMembers(typeCode));
      case EXCEPTION ->
          WireTypeCode.exception(
              typeCode.repositoryId().orElseThrow().value(),
              typeCode.idlName(),
              localMembers(typeCode));
      case ENUM ->
          WireTypeCode.enumeration(
              typeCode.repositoryId().orElseThrow().value(),
              typeCode.idlName(),
              typeCode.enumConstants());
      case TYPEDEF, UNION, NATIVE, VALUE_BOX, VALUETYPE ->
          throw new IllegalArgumentException(
              "wire TypeCode mapping deferred for " + typeCode.kind());
      case SEQUENCE -> WireTypeCode.sequence(fromLocal(typeCode.elementType().orElseThrow()), 0);
    };
  }

  private static List<WireTypeCodeMember> localMembers(IdlTypeCode typeCode) {
    return typeCode.members().stream()
        .map(member -> new WireTypeCodeMember(member.name(), fromLocal(member.type())))
        .toList();
  }
}
