package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.any.AnyAggregateValue;
import io.github.mundanej.mjo.any.AnyException;
import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import io.github.mundanej.mjo.typecode.IdlTypeCodeKind;
import io.github.mundanej.mjo.typecode.IdlTypeCodeMember;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Factory for local DynamicAny values over descriptor-backed Any values. */
public final class DynamicAnyFactory {

  private DynamicAnyFactory() {}

  /** Wraps an existing local Any value after checking that the TypeCode kind is supported. */
  public static DynamicAny fromAny(AnyValue<?> any) {
    Objects.requireNonNull(any, "any");
    requireSupported(any.typeCode());
    return new DynamicAny(any);
  }

  /** Creates a scalar, enum, or long-double dynamic value. */
  public static DynamicAny value(IdlTypeCode typeCode, Object value) {
    requireSupported(typeCode);
    if (typeCode.isAggregate() || typeCode.kind() == IdlTypeCodeKind.SEQUENCE) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "aggregate and sequence DynamicAny values require structured factory methods");
    }
    Object checkedValue = checkedPayload(typeCode, value);
    return new DynamicAny(new AnyValue<>(typeCode, checkedValue));
  }

  /** Creates a struct or exception dynamic value from dynamic members keyed by IDL member name. */
  public static DynamicAny aggregate(IdlTypeCode typeCode, Map<String, DynamicAny> members) {
    requireSupported(typeCode);
    Map<String, DynamicAny> checkedMembers = Objects.requireNonNull(members, "members");
    if (!typeCode.isAggregate()) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "aggregate DynamicAny requires struct or exception TypeCode: " + typeCode.kind());
    }
    Map<String, AnyValue<?>> anyMembers = new LinkedHashMap<>();
    for (IdlTypeCodeMember member : typeCode.members()) {
      DynamicAny value = checkedMembers.get(member.name());
      if (value == null) {
        throw new DynamicException(
            DynamicDiagnosticCodes.INVALID_ARGUMENTS,
            "missing dynamic aggregate member: " + member.name());
      }
      requireType(member.type(), value.any());
      anyMembers.put(member.name(), value.any());
    }
    for (String name : checkedMembers.keySet()) {
      if (typeCode.members().stream().noneMatch(member -> member.name().equals(name))) {
        throw new DynamicException(
            DynamicDiagnosticCodes.INVALID_ARGUMENTS, "unknown dynamic aggregate member: " + name);
      }
    }
    try {
      return new DynamicAny(new AnyValue<>(typeCode, new AnyAggregateValue(typeCode, anyMembers)));
    } catch (AnyException exception) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS, exception.getMessage(), exception);
    }
  }

  /** Creates a sequence dynamic value from dynamic element values. */
  public static DynamicAny sequence(IdlTypeCode typeCode, List<DynamicAny> elements) {
    requireSupported(typeCode);
    if (typeCode.kind() != IdlTypeCodeKind.SEQUENCE) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "sequence DynamicAny requires sequence TypeCode: " + typeCode.kind());
    }
    IdlTypeCode elementType = typeCode.elementType().orElseThrow();
    List<AnyValue<?>> values = new ArrayList<>();
    for (DynamicAny element : Objects.requireNonNull(elements, "elements")) {
      DynamicAny checked = Objects.requireNonNull(element, "element");
      requireType(elementType, checked.any());
      values.add(checked.any());
    }
    return new DynamicAny(new AnyValue<>(typeCode, List.copyOf(values)));
  }

  static void requireType(IdlTypeCode expected, AnyValue<?> actual) {
    Objects.requireNonNull(expected, "expected");
    Objects.requireNonNull(actual, "actual");
    if (!expected.equals(actual.typeCode())) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "expected TypeCode " + expected.kind() + ", got " + actual.typeCode().kind());
    }
  }

  static void requireSupported(IdlTypeCode typeCode) {
    Objects.requireNonNull(typeCode, "typeCode");
    if (typeCode.kind() == IdlTypeCodeKind.VOID) {
      throw new DynamicException(
          DynamicDiagnosticCodes.UNSUPPORTED_TYPE,
          "unsupported DynamicAny TypeCode kind: " + typeCode.kind());
    }
  }

  static void requireValidAnyPayload(IdlTypeCode typeCode, Object value) {
    requireSupported(typeCode);
    if (value == null) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS, "value must not be null");
    }
    if (typeCode.kind() == IdlTypeCodeKind.SEQUENCE) {
      requireAnySequencePayload(typeCode, value);
      return;
    }
    checkedPayload(typeCode, value);
  }

  static Object checkedPayload(IdlTypeCode typeCode, Object value) {
    requireSupported(typeCode);
    if (value == null) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS, "value must not be null");
    }
    return switch (typeCode.kind()) {
      case BOOLEAN -> requireBoolean(typeCode, value);
      case OCTET -> requireIntegerRange(typeCode, value, 0, 0xFFL);
      case CHAR -> requireChar(typeCode, value);
      case SHORT -> requireShort(typeCode, value);
      case UNSIGNED_SHORT -> requireIntegerRange(typeCode, value, 0, 0xFFFFL);
      case LONG -> requireInteger(typeCode, value);
      case UNSIGNED_LONG -> requireLongRange(typeCode, value, 0L, 0xFFFF_FFFFL);
      case LONG_LONG -> requireLong(typeCode, value);
      case UNSIGNED_LONG_LONG -> requireUnsignedLongLong(typeCode, value);
      case FLOAT -> requireFloat(typeCode, value);
      case DOUBLE -> requireDouble(typeCode, value);
      case LONG_DOUBLE -> requireLongDouble(value);
      case STRING -> requireString(typeCode, value);
      case ENUM -> requireEnumLabel(typeCode, value);
      case STRUCT, EXCEPTION -> requireAggregatePayload(typeCode, value);
      case SEQUENCE -> requireSequencePayload(typeCode, value);
      case INTERFACE -> requireIor(typeCode, value);
      case VOID ->
          throw new DynamicException(
              DynamicDiagnosticCodes.UNSUPPORTED_TYPE,
              "unsupported DynamicAny TypeCode kind: " + typeCode.kind());
    };
  }

  private static Boolean requireBoolean(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof Boolean bool)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be boolean");
    }
    return bool;
  }

  private static Integer requireIntegerRange(
      IdlTypeCode typeCode, Object value, long minimum, long maximum) {
    if (!(value instanceof Integer integer) || integer < minimum || integer > maximum) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be an int in range");
    }
    return integer;
  }

  private static Short requireShort(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof Short number)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be short");
    }
    return number;
  }

  private static Integer requireInteger(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof Integer number)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH, "payload for " + typeCode.kind() + " must be int");
    }
    return number;
  }

  private static Character requireChar(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof Character character) || character > 0x00FF) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be a one-octet char");
    }
    return character;
  }

  private static Long requireLong(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof Long number)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH, "payload for " + typeCode.kind() + " must be long");
    }
    return number;
  }

  private static Long requireLongRange(
      IdlTypeCode typeCode, Object value, long minimum, long maximum) {
    if (!(value instanceof Long number) || number < minimum || number > maximum) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be a long in range");
    }
    return number;
  }

  private static BigInteger requireUnsignedLongLong(IdlTypeCode typeCode, Object value) {
    BigInteger limit = BigInteger.ONE.shiftLeft(64);
    if (!(value instanceof BigInteger number)
        || number.signum() < 0
        || number.compareTo(limit) >= 0) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be an unsigned 64-bit BigInteger");
    }
    return number;
  }

  private static Float requireFloat(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof Float number)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be float");
    }
    return number;
  }

  private static Double requireDouble(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof Double number)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be double");
    }
    return number;
  }

  private static byte[] requireLongDouble(Object value) {
    if (!(value instanceof byte[] bytes) || bytes.length != 16) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS,
          "long double dynamic value must be exactly 16 octets");
    }
    return Arrays.copyOf(bytes, bytes.length);
  }

  private static String requireString(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof String string)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be string");
    }
    return string;
  }

  private static String requireEnumLabel(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof String label) || !typeCode.enumConstants().contains(label)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS, "invalid enum dynamic value: " + value);
    }
    return label;
  }

  private static AnyAggregateValue requireAggregatePayload(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof AnyAggregateValue aggregate) || !typeCode.equals(aggregate.typeCode())) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be an aggregate value with matching TypeCode");
    }
    for (IdlTypeCodeMember member : typeCode.members()) {
      AnyValue<?> memberValue = aggregate.member(member.name());
      requireType(member.type(), memberValue);
      checkedPayload(member.type(), memberValue.value());
    }
    return aggregate;
  }

  private static List<?> requireSequencePayload(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof List<?> elements)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be a list");
    }
    IdlTypeCode elementType = typeCode.elementType().orElseThrow();
    List<Object> checkedElements = new ArrayList<>(elements.size());
    for (Object element : elements) {
      checkedElements.add(checkedPayload(elementType, element));
    }
    return List.copyOf(checkedElements);
  }

  private static List<AnyValue<?>> requireAnySequencePayload(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof List<?> elements)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be a list");
    }
    IdlTypeCode elementType = typeCode.elementType().orElseThrow();
    List<AnyValue<?>> checkedElements = new ArrayList<>(elements.size());
    for (Object element : elements) {
      if (!(element instanceof AnyValue<?> any)) {
        throw new DynamicException(
            DynamicDiagnosticCodes.TYPE_MISMATCH,
            "dynamic sequence payload must contain AnyValue elements");
      }
      requireType(elementType, any);
      checkedPayload(elementType, any.value());
      checkedElements.add(any);
    }
    return List.copyOf(checkedElements);
  }

  private static Ior requireIor(IdlTypeCode typeCode, Object value) {
    if (!(value instanceof Ior ior)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "payload for " + typeCode.kind() + " must be an IOR object reference");
    }
    return ior;
  }
}
