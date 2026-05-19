package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Classifies explicit Java remote-interface declarations for Java-to-IDL eligibility. */
public final class RmiJavaEligibilityChecker {

  private static final String REMOTE_EXCEPTION = "java.rmi.RemoteException";
  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
  private static final Set<String> JAVA_KEYWORDS =
      Set.of(
          "abstract",
          "assert",
          "boolean",
          "break",
          "byte",
          "case",
          "catch",
          "char",
          "class",
          "const",
          "continue",
          "default",
          "do",
          "double",
          "else",
          "enum",
          "extends",
          "final",
          "finally",
          "float",
          "for",
          "goto",
          "if",
          "implements",
          "import",
          "instanceof",
          "int",
          "interface",
          "long",
          "native",
          "new",
          "package",
          "private",
          "protected",
          "public",
          "return",
          "short",
          "static",
          "strictfp",
          "super",
          "switch",
          "synchronized",
          "this",
          "throw",
          "throws",
          "transient",
          "try",
          "void",
          "volatile",
          "while",
          "_");
  private static final Set<String> PRIMITIVES =
      Set.of("boolean", "byte", "char", "double", "float", "int", "long", "short");

  /** Creates a stateless eligibility checker. */
  public RmiJavaEligibilityChecker() {}

  /** Checks whether a declaration belongs to the approved G7-010 Java-to-IDL input slice. */
  public RmiJavaEligibilityResult check(RmiJavaRemoteInterface declaration) {
    if (declaration == null) {
      return rejected(
          RmiJavaDiagnosticCodes.NULL_DECLARATION, "Remote interface declaration is required.");
    }

    List<Diagnostic> diagnostics = new ArrayList<>();
    validateInterface(declaration, diagnostics);
    validateOperations(declaration.operations(), diagnostics);
    if (hasErrors(diagnostics)) {
      return new RmiJavaEligibilityResult(Optional.empty(), diagnostics);
    }
    return new RmiJavaEligibilityResult(Optional.of(declaration), diagnostics);
  }

  private static void validateInterface(
      RmiJavaRemoteInterface declaration, List<Diagnostic> diagnostics) {
    if (!isBinaryName(declaration.binaryName())) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.INVALID_INTERFACE_NAME,
          "Remote interface binary name is not a valid Java binary name: "
              + printable(declaration.binaryName()));
    }
    if (!declaration.remote()) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.NON_REMOTE_INTERFACE,
          "Remote interface declaration must explicitly be marked remote-compatible.");
    }
  }

  private static void validateOperations(
      List<RmiJavaOperation> operations, List<Diagnostic> diagnostics) {
    Set<String> operationNames = new HashSet<>();
    for (RmiJavaOperation operation : operations) {
      if (!isIdentifier(operation.name())) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.INVALID_OPERATION_NAME,
            "Operation name is not a valid Java identifier: " + printable(operation.name()));
      }
      if (operation.kind() != RmiJavaOperationKind.ABSTRACT) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.UNSUPPORTED_OPERATION_KIND,
            "Only abstract remote-interface operations are eligible: " + operation.name());
      }
      if (operation.varargs()) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.UNSUPPORTED_VARARGS,
            "Varargs operation is outside the approved Java-to-IDL eligibility slice: "
                + operation.name());
      }
      if (!operationNames.add(operation.name())) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.DUPLICATE_OPERATION_NAME,
            "Overloaded or duplicate operation name is deferred to G7-020: " + operation.name());
      }
      validateType(operation.returnType(), TypePosition.RETURN, operation.name(), diagnostics);
      validateParameters(operation, diagnostics);
      validateExceptions(operation, diagnostics);
    }
  }

  private static void validateParameters(RmiJavaOperation operation, List<Diagnostic> diagnostics) {
    for (RmiJavaParameter parameter : operation.parameters()) {
      if (!isIdentifier(parameter.name())) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.INVALID_PARAMETER_NAME,
            "Parameter name is not a valid Java identifier: " + printable(parameter.name()));
      }
      validateType(parameter.type(), TypePosition.PARAMETER, operation.name(), diagnostics);
    }
  }

  private static void validateExceptions(RmiJavaOperation operation, List<Diagnostic> diagnostics) {
    boolean declaresRemoteException = false;
    for (RmiJavaTypeReference exceptionType : operation.exceptions()) {
      if (exceptionType.kind() != RmiJavaTypeKind.DECLARED || !isBinaryName(exceptionType.name())) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.INVALID_EXCEPTION_TYPE,
            "Exception type reference is not an explicit Java binary name: "
                + exceptionType.displayName());
        continue;
      }
      if (REMOTE_EXCEPTION.equals(exceptionType.name())) {
        declaresRemoteException = true;
      }
    }
    if (!declaresRemoteException) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.MISSING_REMOTE_EXCEPTION,
          "Operation must explicitly declare java.rmi.RemoteException: " + operation.name());
    }
  }

  private static void validateType(
      RmiJavaTypeReference type,
      TypePosition position,
      String operationName,
      List<Diagnostic> diagnostics) {
    if (type.kind() == RmiJavaTypeKind.VOID) {
      if (position != TypePosition.RETURN) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
            "void is only valid as an operation return type: " + operationName);
      }
      return;
    }
    if (type.kind() == RmiJavaTypeKind.PRIMITIVE) {
      if (!PRIMITIVES.contains(type.name())) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
            "Primitive type is not supported by the approved slice: " + type.displayName());
      }
      return;
    }
    if (type.kind() == RmiJavaTypeKind.DECLARED) {
      if (!isBinaryName(type.name())) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
            "Declared type is not a valid Java binary name: " + type.displayName());
      }
      return;
    }
    emit(
        diagnostics,
        RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
        "Type shape is deferred to a later Java-to-IDL task: " + type.displayName());
  }

  private static boolean hasErrors(List<Diagnostic> diagnostics) {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }

  private static RmiJavaEligibilityResult rejected(DiagnosticCode code, String message) {
    return new RmiJavaEligibilityResult(
        Optional.empty(), List.of(Diagnostic.withoutSpan(code, DiagnosticSeverity.ERROR, message)));
  }

  private static void emit(List<Diagnostic> diagnostics, DiagnosticCode code, String message) {
    diagnostics.add(Diagnostic.withoutSpan(code, DiagnosticSeverity.ERROR, message));
  }

  private static boolean isBinaryName(String value) {
    Objects.requireNonNull(value, "value");
    String[] parts = value.split("\\.", -1);
    if (parts.length == 0) {
      return false;
    }
    for (String part : parts) {
      if (!isIdentifier(part)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isIdentifier(String value) {
    Objects.requireNonNull(value, "value");
    return IDENTIFIER_PATTERN.matcher(value).matches() && !JAVA_KEYWORDS.contains(value);
  }

  private static String printable(String value) {
    return value.isBlank() ? "<blank>" : value;
  }

  private enum TypePosition {
    RETURN,
    PARAMETER
  }
}
