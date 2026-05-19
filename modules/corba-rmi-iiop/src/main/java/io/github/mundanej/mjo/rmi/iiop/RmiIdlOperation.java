package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * IDL operation model derived from an eligible Java remote method.
 *
 * @param name IDL operation identifier
 * @param returnType mapped IDL return type
 * @param parameters mapped parameters in Java declaration order
 * @param exceptions mapped user exceptions, excluding {@code java.rmi.RemoteException}
 */
public record RmiIdlOperation(
    String name,
    RmiIdlTypeReference returnType,
    List<RmiIdlParameter> parameters,
    List<RmiIdlExceptionReference> exceptions) {

  /** Creates an immutable operation model. */
  public RmiIdlOperation {
    name = requireNonBlank(name, "name");
    Objects.requireNonNull(returnType, "returnType");
    parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
    exceptions = List.copyOf(Objects.requireNonNull(exceptions, "exceptions"));
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
