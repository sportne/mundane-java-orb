package io.github.mundanej.mjo.notification;

/** Immutable primitive value for the supported local structured-event subset. */
public record NotificationPrimitiveValue(NotificationPrimitiveKind kind, Object value) {

  /** Maximum supported string length for primitive values. */
  public static final int MAX_STRING_LENGTH = 1_024;

  /** Creates a validated primitive value. */
  public NotificationPrimitiveValue {
    kind = requirePresent("kind", kind);
    value = validate(kind, value);
  }

  /** Creates a bounded string value. */
  public static NotificationPrimitiveValue stringValue(String value) {
    return new NotificationPrimitiveValue(NotificationPrimitiveKind.STRING, value);
  }

  /** Creates a boolean value. */
  public static NotificationPrimitiveValue booleanValue(boolean value) {
    return new NotificationPrimitiveValue(
        NotificationPrimitiveKind.BOOLEAN, Boolean.valueOf(value));
  }

  /** Creates a signed 64-bit integer value. */
  public static NotificationPrimitiveValue signedLongValue(long value) {
    return new NotificationPrimitiveValue(
        NotificationPrimitiveKind.SIGNED_LONG, Long.valueOf(value));
  }

  /** Creates a finite floating-point value. */
  public static NotificationPrimitiveValue floatingPointValue(double value) {
    return new NotificationPrimitiveValue(
        NotificationPrimitiveKind.FLOATING_POINT, Double.valueOf(value));
  }

  /** Returns this value as a string. */
  public String asString() {
    return (String) requireKind(NotificationPrimitiveKind.STRING);
  }

  /** Returns this value as a boolean. */
  public boolean asBoolean() {
    return ((Boolean) requireKind(NotificationPrimitiveKind.BOOLEAN)).booleanValue();
  }

  /** Returns this value as a signed 64-bit integer. */
  public long asSignedLong() {
    return ((Long) requireKind(NotificationPrimitiveKind.SIGNED_LONG)).longValue();
  }

  /** Returns this value as a finite floating-point value. */
  public double asFloatingPoint() {
    return ((Double) requireKind(NotificationPrimitiveKind.FLOATING_POINT)).doubleValue();
  }

  private Object requireKind(NotificationPrimitiveKind expected) {
    if (kind != expected) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.UNSUPPORTED_VALUE,
          "primitive value kind is " + kind + ", not " + expected);
    }
    return value;
  }

  private static Object validate(NotificationPrimitiveKind kind, Object value) {
    requirePresent("value", value);
    return switch (kind) {
      case STRING -> validateString(value);
      case BOOLEAN -> requireType(value, Boolean.class, kind);
      case SIGNED_LONG -> requireType(value, Long.class, kind);
      case FLOATING_POINT -> validateFloatingPoint(value);
    };
  }

  private static String validateString(Object value) {
    String string = requireType(value, String.class, NotificationPrimitiveKind.STRING);
    if (string.length() > MAX_STRING_LENGTH) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.VALUE_LIMIT_EXCEEDED,
          "structured-event string value exceeds " + MAX_STRING_LENGTH + " characters");
    }
    return string;
  }

  private static Double validateFloatingPoint(Object value) {
    Double number = requireType(value, Double.class, NotificationPrimitiveKind.FLOATING_POINT);
    if (!Double.isFinite(number.doubleValue())) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.UNSUPPORTED_VALUE,
          "floating-point structured-event values must be finite");
    }
    return number;
  }

  private static <T> T requireType(
      Object value, Class<T> expectedType, NotificationPrimitiveKind kind) {
    if (!expectedType.isInstance(value)) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.UNSUPPORTED_VALUE,
          "unsupported Java value for " + kind + ": " + value.getClass().getName());
    }
    return expectedType.cast(value);
  }

  private static <T> T requirePresent(String name, T value) {
    if (value == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.UNSUPPORTED_VALUE,
          "primitive value " + name + " must not be null");
    }
    return value;
  }
}
