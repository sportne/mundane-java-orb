package io.github.mundanej.mjo.idl.java.mapping;

import java.util.List;
import java.util.Objects;

/**
 * Java method selected from an IDL operation.
 *
 * @param returnType Java return type spelling
 * @param name Java method name
 * @param parameters parameters in deterministic IDL order
 * @param thrownTypes Java exception type spellings from IDL raises clauses
 * @param factory whether the operation represents an IDL valuetype factory
 */
public record JavaMappedOperation(
    String returnType,
    String name,
    List<JavaMappedParameter> parameters,
    List<String> thrownTypes,
    boolean factory) {

  /** Creates a validated mapped operation. */
  public JavaMappedOperation {
    returnType = requireNonBlank(returnType, "returnType");
    name = requireNonBlank(name, "name");
    parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
    thrownTypes = List.copyOf(Objects.requireNonNull(thrownTypes, "thrownTypes"));
  }

  /** Creates a normal mapped operation. */
  public JavaMappedOperation(
      String returnType,
      String name,
      List<JavaMappedParameter> parameters,
      List<String> thrownTypes) {
    this(returnType, name, parameters, thrownTypes, false);
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
