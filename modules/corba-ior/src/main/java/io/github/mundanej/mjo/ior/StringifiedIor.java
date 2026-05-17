package io.github.mundanej.mjo.ior;

import java.util.Locale;
import java.util.Objects;

/** Parser and emitter for standard {@code IOR:} stringified object references. */
public final class StringifiedIor {

  private static final String PREFIX = "IOR:";

  private StringifiedIor() {}

  /** Parses a stringified IOR with default bounds. */
  public static Ior parse(String value) {
    return parse(value, IorLimits.defaults());
  }

  /** Parses a stringified IOR with caller-supplied bounds. */
  public static Ior parse(String value, IorLimits limits) {
    Objects.requireNonNull(value, "value");
    requirePrefix(value);
    String hex = value.substring(PREFIX.length());
    if (hex.isEmpty()) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_STRINGIFIED_IOR,
          "stringified IOR hex payload must not be empty");
    }
    if (hex.length() % 2 == 0) {
      limits.requireWithin(limits.encapsulationOctets(), hex.length() / 2L);
    }
    return Ior.fromEncapsulation(IorWire.decodeHex(hex), limits);
  }

  /** Emits a canonical uppercase {@code IOR:} string. */
  public static String format(Ior ior) {
    Objects.requireNonNull(ior, "ior");
    return PREFIX + IorWire.encodeHex(ior.toEncapsulation());
  }

  private static void requirePrefix(String value) {
    if (!value.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_STRINGIFIED_IOR,
          "stringified IOR must start with " + PREFIX.toLowerCase(Locale.ROOT));
    }
  }
}
