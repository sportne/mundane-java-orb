package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * Explicit local RMI-IIOP CDR value paired with its approved IDL type reference.
 *
 * @param type IDL type expected for the payload
 * @param value boxed Java value, or {@code null} for {@code void}
 */
public record RmiCdrValue(RmiIdlTypeReference type, Object value) {

  /** Creates an immutable local CDR value descriptor. */
  public RmiCdrValue {
    Objects.requireNonNull(type, "type");
  }

  /** Returns a void value marker. */
  public static RmiCdrValue voidValue() {
    return new RmiCdrValue(RmiIdlTypeReference.voidType(), null);
  }

  /** Returns a boolean value. */
  public static RmiCdrValue booleanValue(boolean value) {
    return builtin("boolean", value);
  }

  /** Returns an octet value. */
  public static RmiCdrValue octetValue(byte value) {
    return builtin("octet", value);
  }

  /** Returns a wide character value. */
  public static RmiCdrValue wcharValue(char value) {
    return builtin("wchar", value);
  }

  /** Returns a short value. */
  public static RmiCdrValue shortValue(short value) {
    return builtin("short", value);
  }

  /** Returns an IDL long value, corresponding to Java {@code int}. */
  public static RmiCdrValue longValue(int value) {
    return builtin("long", value);
  }

  /** Returns an IDL long long value, corresponding to Java {@code long}. */
  public static RmiCdrValue longLongValue(long value) {
    return builtin("long long", value);
  }

  /** Returns a float value. */
  public static RmiCdrValue floatValue(float value) {
    return builtin("float", value);
  }

  /** Returns a double value. */
  public static RmiCdrValue doubleValue(double value) {
    return builtin("double", value);
  }

  /** Returns a Java string value mapped to IDL {@code wstring}. */
  public static RmiCdrValue stringValue(String value) {
    return builtin("wstring", Objects.requireNonNull(value, "value"));
  }

  /** Returns a sequence value with explicit element type metadata. */
  public static RmiCdrValue sequenceValue(
      RmiIdlTypeReference elementType, List<RmiCdrValue> values) {
    return new RmiCdrValue(
        RmiIdlTypeReference.sequenceOf(Objects.requireNonNull(elementType, "elementType")),
        List.copyOf(Objects.requireNonNull(values, "values")));
  }

  /** Returns a remote object-reference value backed by a deterministic object key. */
  public static RmiCdrValue objectReferenceValue(
      RmiIdlTypeReference remoteType, RmiIiopObjectKey objectKey) {
    if (remoteType.kind() != RmiIdlTypeKind.REMOTE_OBJECT) {
      throw new IllegalArgumentException("remoteType must be REMOTE_OBJECT");
    }
    return new RmiCdrValue(remoteType, Objects.requireNonNull(objectKey, "objectKey"));
  }

  /** Returns a declared value payload with explicit field metadata. */
  public static RmiCdrValue declaredValue(
      RmiIdlTypeReference declaredType, RmiCdrDeclaredValue value) {
    if (declaredType.kind() != RmiIdlTypeKind.DECLARED_VALUE) {
      throw new IllegalArgumentException("declaredType must be DECLARED_VALUE");
    }
    return new RmiCdrValue(declaredType, Objects.requireNonNull(value, "value"));
  }

  private static RmiCdrValue builtin(String idlName, Object value) {
    return new RmiCdrValue(RmiIdlTypeReference.builtin(idlName), value);
  }
}
