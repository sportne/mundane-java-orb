package io.github.mundanej.mjo.common;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable diagnostic identifier.
 *
 * @param value diagnostic code in {@code AREA-0000} form
 */
public record DiagnosticCode(String value) {

  private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z]+-[0-9]{4}");

  /** Creates a validated diagnostic code. */
  public DiagnosticCode {
    value = requireNonBlank(value, "value");
    if (!CODE_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("Diagnostic code must match AREA-0000: " + value);
    }
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
