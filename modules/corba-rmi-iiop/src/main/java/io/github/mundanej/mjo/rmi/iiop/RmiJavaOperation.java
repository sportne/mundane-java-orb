package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * Explicit Java remote-interface operation declaration.
 *
 * @param name Java method name
 * @param returnType declared return type
 * @param parameters declared parameters in source order
 * @param exceptions declared thrown exception types in source order
 * @param kind Java method kind
 * @param varargs whether the declaration uses varargs syntax
 */
public record RmiJavaOperation(
    String name,
    RmiJavaTypeReference returnType,
    List<RmiJavaParameter> parameters,
    List<RmiJavaTypeReference> exceptions,
    RmiJavaOperationKind kind,
    boolean varargs) {

  /** Creates an immutable operation declaration. */
  public RmiJavaOperation {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(returnType, "returnType");
    parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
    exceptions = List.copyOf(Objects.requireNonNull(exceptions, "exceptions"));
    Objects.requireNonNull(kind, "kind");
  }

  /** Creates a normal abstract remote-interface operation declaration. */
  public static RmiJavaOperation abstractOperation(
      String name,
      RmiJavaTypeReference returnType,
      List<RmiJavaParameter> parameters,
      List<RmiJavaTypeReference> exceptions) {
    return new RmiJavaOperation(
        name, returnType, parameters, exceptions, RmiJavaOperationKind.ABSTRACT, false);
  }
}
