package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Generates deterministic parser-supported IDL fixture text from RMI Java-to-IDL models. */
public final class RmiGeneratedIdlFixtureGenerator {

  private static final String SOURCE_NAME = "rmi-generated.idl";

  /** Creates a stateless generated IDL fixture generator. */
  public RmiGeneratedIdlFixtureGenerator() {}

  /** Generates parser-supported IDL text for the supplied Java-to-IDL model. */
  public RmiGeneratedIdlResult generate(RmiIdlTranslationUnit translationUnit) {
    Objects.requireNonNull(translationUnit, "translationUnit");
    List<Diagnostic> diagnostics = new ArrayList<>();
    validateTranslationUnit(translationUnit, diagnostics);
    if (hasErrors(diagnostics)) {
      return new RmiGeneratedIdlResult(Optional.empty(), diagnostics);
    }
    return new RmiGeneratedIdlResult(
        Optional.of(
            new RmiGeneratedIdlFixture(SOURCE_NAME, renderTranslationUnit(translationUnit))),
        diagnostics);
  }

  private static void validateTranslationUnit(
      RmiIdlTranslationUnit translationUnit, List<Diagnostic> diagnostics) {
    validateInterfaces(translationUnit.interfaces(), List.of(), diagnostics);
    for (RmiIdlModule module : translationUnit.modules()) {
      validateModule(module, List.of(module.name()), diagnostics);
    }
  }

  private static void validateModule(
      RmiIdlModule module, List<String> modulePath, List<Diagnostic> diagnostics) {
    validateInterfaces(module.interfaces(), modulePath, diagnostics);
    for (RmiIdlModule nestedModule : module.modules()) {
      List<String> nestedPath = new ArrayList<>(modulePath);
      nestedPath.add(nestedModule.name());
      validateModule(nestedModule, nestedPath, diagnostics);
    }
  }

  private static void validateInterfaces(
      List<RmiIdlInterface> interfaces, List<String> modulePath, List<Diagnostic> diagnostics) {
    for (RmiIdlInterface idlInterface : interfaces) {
      for (RmiIdlOperation operation : idlInterface.operations()) {
        validateType(operation.returnType(), operation.name(), diagnostics);
        for (RmiIdlParameter parameter : operation.parameters()) {
          validateType(parameter.type(), operation.name() + "." + parameter.name(), diagnostics);
        }
        for (RmiIdlExceptionReference exception : operation.exceptions()) {
          List<String> exceptionPath = scopedParts(exception.scopedName());
          if (exceptionPath.size() != modulePath.size() + 1
              || !exceptionPath.subList(0, modulePath.size()).equals(modulePath)) {
            emit(
                diagnostics,
                RmiJavaDiagnosticCodes.UNSUPPORTED_GENERATED_IDL_EXCEPTION_SCOPE,
                "Generated IDL fixture exception must be declared beside the operation: "
                    + exception.scopedName());
          }
        }
      }
    }
  }

  private static void validateType(
      RmiIdlTypeReference type, String context, List<Diagnostic> diagnostics) {
    if (type.kind() == RmiIdlTypeKind.SEQUENCE) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.UNSUPPORTED_GENERATED_IDL_SEQUENCE,
          "Generated IDL fixtures do not emit sequence types in G7-040: " + context);
    } else if (type.kind() == RmiIdlTypeKind.DECLARED_VALUE) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.UNSUPPORTED_GENERATED_IDL_DECLARED_TYPE,
          "Generated IDL fixtures do not emit declared value/reference types in G7-040: "
              + context);
    }
  }

  private static String renderTranslationUnit(RmiIdlTranslationUnit translationUnit) {
    StringBuilder output = new StringBuilder();
    for (RmiIdlModule module : translationUnit.modules()) {
      renderModule(module, 0, output);
    }
    renderInterfaces(translationUnit.interfaces(), List.of(), 0, output);
    return output.toString();
  }

  private static void renderModule(RmiIdlModule module, int indent, StringBuilder output) {
    line(output, indent, "module " + module.name() + " {");
    for (RmiIdlModule nestedModule : module.modules()) {
      renderModule(nestedModule, indent + 1, output);
    }
    renderInterfaces(module.interfaces(), modulePath(module.scopedName()), indent + 1, output);
    line(output, indent, "};");
  }

  private static void renderInterfaces(
      List<RmiIdlInterface> interfaces, List<String> modulePath, int indent, StringBuilder output) {
    Set<String> emittedExceptions = new LinkedHashSet<>();
    for (RmiIdlInterface idlInterface : interfaces) {
      for (RmiIdlOperation operation : idlInterface.operations()) {
        for (RmiIdlExceptionReference exception : operation.exceptions()) {
          if (emittedExceptions.add(exception.scopedName())) {
            line(output, indent, "exception " + simpleName(exception.scopedName()) + " {");
            for (RmiIdlValueMember field : exception.fields()) {
              line(output, indent + 1, typeName(field.type()) + " " + field.name() + ";");
            }
            line(output, indent, "};");
          }
        }
      }
    }
    for (RmiIdlInterface idlInterface : interfaces) {
      renderInterface(idlInterface, modulePath, indent, output);
    }
  }

  private static void renderInterface(
      RmiIdlInterface idlInterface, List<String> modulePath, int indent, StringBuilder output) {
    String inheritance =
        idlInterface.baseScopedNames().isEmpty()
            ? ""
            : " : " + String.join(", ", idlInterface.baseScopedNames());
    line(output, indent, "interface " + idlInterface.name() + inheritance + " {");
    for (RmiIdlOperation operation : idlInterface.operations()) {
      renderOperation(operation, modulePath, indent + 1, output);
    }
    line(output, indent, "};");
  }

  private static void renderOperation(
      RmiIdlOperation operation, List<String> modulePath, int indent, StringBuilder output) {
    StringBuilder line = new StringBuilder();
    line.append(typeName(operation.returnType())).append(' ').append(operation.name()).append('(');
    for (int index = 0; index < operation.parameters().size(); index++) {
      if (index > 0) {
        line.append(", ");
      }
      RmiIdlParameter parameter = operation.parameters().get(index);
      line.append("in ").append(typeName(parameter.type())).append(' ').append(parameter.name());
    }
    line.append(')');
    if (!operation.exceptions().isEmpty()) {
      line.append(" raises (");
      for (int index = 0; index < operation.exceptions().size(); index++) {
        if (index > 0) {
          line.append(", ");
        }
        line.append(exceptionReference(operation.exceptions().get(index), modulePath));
      }
      line.append(')');
    }
    line.append(';');
    line(output, indent, line.toString());
  }

  private static String typeName(RmiIdlTypeReference type) {
    return type.name();
  }

  private static String exceptionReference(
      RmiIdlExceptionReference exception, List<String> modulePath) {
    List<String> exceptionParts = scopedParts(exception.scopedName());
    if (exceptionParts.subList(0, modulePath.size()).equals(modulePath)) {
      return exceptionParts.getLast();
    }
    return exception.scopedName();
  }

  private static List<String> modulePath(String scopedName) {
    return scopedParts(scopedName);
  }

  private static List<String> scopedParts(String scopedName) {
    String normalized = scopedName.startsWith("::") ? scopedName.substring(2) : scopedName;
    if (normalized.isBlank()) {
      return List.of();
    }
    return List.of(normalized.split("::"));
  }

  private static String simpleName(String scopedName) {
    return scopedParts(scopedName).getLast();
  }

  private static void line(StringBuilder output, int indent, String value) {
    output.append("  ".repeat(indent)).append(value).append('\n');
  }

  private static boolean hasErrors(List<Diagnostic> diagnostics) {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }

  private static void emit(List<Diagnostic> diagnostics, DiagnosticCode code, String message) {
    diagnostics.add(Diagnostic.withoutSpan(code, DiagnosticSeverity.ERROR, message));
  }
}
