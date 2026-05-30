package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for an IDL operation declaration.
 *
 * @param oneway whether the operation uses the `oneway` modifier
 * @param returnType operation return type, including `void`
 * @param name operation identifier
 * @param parameters parameters in encounter order
 * @param raises exception scoped names listed in a `raises` clause
 * @param contexts operation context string literals in encounter order
 * @param span source span covered by the operation declaration
 */
public record IdlOperation(
    boolean oneway,
    IdlTypeReference returnType,
    String name,
    List<IdlParameter> parameters,
    List<String> raises,
    List<String> contexts,
    SourceSpan span)
    implements IdlInterfaceMember, IdlValueMember {

  /** Creates a validated operation node. */
  public IdlOperation {
    Objects.requireNonNull(returnType, "returnType");
    name = IdlAstValidation.requireNonBlank(name, "name");
    parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
    raises = List.copyOf(Objects.requireNonNull(raises, "raises"));
    contexts = List.copyOf(Objects.requireNonNull(contexts, "contexts"));
    contexts.forEach(context -> IdlAstValidation.requireNonBlank(context, "contexts"));
    Objects.requireNonNull(span, "span");
  }

  /** Creates an operation without context clauses. */
  public IdlOperation(
      boolean oneway,
      IdlTypeReference returnType,
      String name,
      List<IdlParameter> parameters,
      List<String> raises,
      SourceSpan span) {
    this(oneway, returnType, name, parameters, raises, List.of(), span);
  }
}
