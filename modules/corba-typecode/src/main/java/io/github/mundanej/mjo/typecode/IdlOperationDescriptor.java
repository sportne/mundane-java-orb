package io.github.mundanej.mjo.typecode;

import java.util.List;
import java.util.Objects;

/**
 * Static descriptor for an IDL operation.
 *
 * @param name IDL operation name
 * @param returnType operation return type
 * @param parameters operation parameters in IDL encounter order
 * @param raises exception types listed in the raises clause
 */
public record IdlOperationDescriptor(
    String name,
    IdlTypeReference returnType,
    List<IdlParameterDescriptor> parameters,
    List<IdlTypeReference> raises) {

  /** Creates a validated operation descriptor. */
  public IdlOperationDescriptor {
    name = requireNonBlank(name, "name");
    Objects.requireNonNull(returnType, "returnType");
    parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
    raises = List.copyOf(Objects.requireNonNull(raises, "raises"));
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
