package io.github.mundanej.mjo.nativeimage;

import java.util.Objects;

/** One Native Image smoke binary target. */
public record NativeImageTarget(String name, String mainClass, String binaryName) {

  /** Creates a validated target. */
  public NativeImageTarget {
    name = requireNonBlank(name, "name");
    mainClass = requireNonBlank(mainClass, "mainClass");
    binaryName = requireNonBlank(binaryName, "binaryName");
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName);
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
