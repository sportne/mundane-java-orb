package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Maps explicit eligible Java remote-interface declarations into an IDL model. */
public final class RmiJavaToIdlMapper {

  private static final String REMOTE_EXCEPTION = "java.rmi.RemoteException";
  private static final Pattern IDL_IDENTIFIER_PATTERN = Pattern.compile("_?[A-Za-z][A-Za-z0-9_]*");
  private static final Set<String> IDL_RESERVED_WORDS =
      Set.of(
          "any",
          "attribute",
          "boolean",
          "case",
          "char",
          "const",
          "context",
          "default",
          "double",
          "enum",
          "exception",
          "fixed",
          "float",
          "in",
          "inout",
          "interface",
          "long",
          "module",
          "Object",
          "octet",
          "oneway",
          "out",
          "raises",
          "readonly",
          "sequence",
          "short",
          "string",
          "struct",
          "switch",
          "typedef",
          "union",
          "unsigned",
          "ValueBase",
          "void",
          "wchar",
          "wstring");

  private final RmiJavaEligibilityChecker eligibilityChecker;

  /** Creates a mapper with the default eligibility checker. */
  public RmiJavaToIdlMapper() {
    this(new RmiJavaEligibilityChecker());
  }

  /** Creates a mapper with an explicit eligibility checker. */
  public RmiJavaToIdlMapper(RmiJavaEligibilityChecker eligibilityChecker) {
    this.eligibilityChecker = Objects.requireNonNull(eligibilityChecker, "eligibilityChecker");
  }

  /** Maps one Java remote-interface declaration into an immutable Java-to-IDL model. */
  public RmiJavaToIdlResult map(RmiJavaRemoteInterface declaration) {
    RmiJavaEligibilityResult eligibility = eligibilityChecker.check(declaration);
    List<Diagnostic> diagnostics = new ArrayList<>(eligibility.diagnostics());
    if (declaration != null) {
      collectMappingDiagnostics(declaration, diagnostics);
    }
    if (hasErrors(diagnostics)) {
      return new RmiJavaToIdlResult(Optional.empty(), diagnostics);
    }
    return new RmiJavaToIdlResult(
        Optional.of(buildTranslationUnit(eligibility.remoteInterface().orElseThrow())),
        diagnostics);
  }

  private static void collectMappingDiagnostics(
      RmiJavaRemoteInterface declaration, List<Diagnostic> diagnostics) {
    List<String> binaryParts = List.of(declaration.binaryName().split("\\.", -1));
    if (binaryParts.stream().noneMatch(String::isBlank)) {
      for (int index = 0; index < binaryParts.size() - 1; index++) {
        String moduleName = binaryParts.get(index);
        if (!isIdlIdentifier(moduleName)) {
          emit(
              diagnostics,
              RmiJavaDiagnosticCodes.INVALID_IDL_MODULE_NAME,
              "Java package segment cannot be used as an IDL module name: " + moduleName);
        }
      }
      String interfaceName = binaryParts.get(binaryParts.size() - 1);
      if (!isIdlIdentifier(interfaceName)) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.INVALID_IDL_INTERFACE_NAME,
            "Java interface name cannot be used as an IDL interface name: " + interfaceName);
      }
    }

    for (RmiJavaOperation operation : declaration.operations()) {
      if (!isIdlIdentifier(operation.name())) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.INVALID_IDL_OPERATION_NAME,
            "Java method name cannot be used as an IDL operation name: " + operation.name());
      }
      collectTypeDiagnostics(operation.returnType(), diagnostics);
      for (RmiJavaParameter parameter : operation.parameters()) {
        if (!isIdlIdentifier(parameter.name())) {
          emit(
              diagnostics,
              RmiJavaDiagnosticCodes.INVALID_IDL_PARAMETER_NAME,
              "Java parameter name cannot be used as an IDL parameter name: " + parameter.name());
        }
        collectTypeDiagnostics(parameter.type(), diagnostics);
      }
      for (RmiJavaTypeReference exceptionType : operation.exceptions()) {
        if (!REMOTE_EXCEPTION.equals(exceptionType.name())) {
          collectExceptionDiagnostics(exceptionType, diagnostics);
        }
      }
    }
  }

  private static void collectTypeDiagnostics(
      RmiJavaTypeReference type, List<Diagnostic> diagnostics) {
    if (type.kind() == RmiJavaTypeKind.GENERIC || type.kind() == RmiJavaTypeKind.WILDCARD) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.UNSUPPORTED_IDL_TYPE_MAPPING,
          "Java type shape cannot be mapped to IDL in G7-020: " + type.displayName());
      return;
    }
    if (type.kind() == RmiJavaTypeKind.ARRAY) {
      if (type.componentType().isEmpty()) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.UNSUPPORTED_IDL_TYPE_MAPPING,
            "Java array type requires component metadata for IDL sequence mapping: "
                + type.displayName());
        return;
      }
      collectTypeDiagnostics(type.componentType().orElseThrow(), diagnostics);
      return;
    }
    if (type.kind() == RmiJavaTypeKind.DECLARED && !"java.lang.String".equals(type.name())) {
      validateScopedJavaName(
          type.name(),
          RmiJavaDiagnosticCodes.UNSUPPORTED_IDL_TYPE_MAPPING,
          "Java declared type cannot be mapped to an IDL scoped name: ",
          diagnostics);
    }
  }

  private static void collectExceptionDiagnostics(
      RmiJavaTypeReference exceptionType, List<Diagnostic> diagnostics) {
    if (exceptionType.kind() != RmiJavaTypeKind.DECLARED) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.INVALID_IDL_EXCEPTION_NAME,
          "Java exception type cannot be mapped to an IDL exception name: "
              + exceptionType.displayName());
      return;
    }
    validateScopedJavaName(
        exceptionType.name(),
        RmiJavaDiagnosticCodes.INVALID_IDL_EXCEPTION_NAME,
        "Java exception type cannot be mapped to an IDL exception name: ",
        diagnostics);
  }

  private static void validateScopedJavaName(
      String javaBinaryName,
      DiagnosticCode code,
      String messagePrefix,
      List<Diagnostic> diagnostics) {
    List<String> parts = List.of(javaBinaryName.split("\\.", -1));
    if (parts.stream().anyMatch(part -> !isIdlIdentifier(part))) {
      emit(diagnostics, code, messagePrefix + javaBinaryName);
    }
  }

  private static RmiIdlTranslationUnit buildTranslationUnit(RmiJavaRemoteInterface declaration) {
    List<String> binaryParts = List.of(declaration.binaryName().split("\\."));
    String interfaceName = binaryParts.get(binaryParts.size() - 1);
    List<String> modulePath = binaryParts.subList(0, binaryParts.size() - 1);
    RmiIdlInterface idlInterface =
        new RmiIdlInterface(
            interfaceName,
            scopedName(binaryParts),
            Optional.of(declaration.binaryName()),
            mapOperations(declaration.operations()));
    if (modulePath.isEmpty()) {
      return new RmiIdlTranslationUnit(List.of(), List.of(idlInterface));
    }
    return new RmiIdlTranslationUnit(List.of(buildModule(modulePath, 0, idlInterface)), List.of());
  }

  private static RmiIdlModule buildModule(
      List<String> modulePath, int index, RmiIdlInterface idlInterface) {
    List<String> scopedParts = modulePath.subList(0, index + 1);
    if (index == modulePath.size() - 1) {
      return new RmiIdlModule(
          modulePath.get(index), scopedName(scopedParts), List.of(), List.of(idlInterface));
    }
    return new RmiIdlModule(
        modulePath.get(index),
        scopedName(scopedParts),
        List.of(buildModule(modulePath, index + 1, idlInterface)),
        List.of());
  }

  private static List<RmiIdlOperation> mapOperations(List<RmiJavaOperation> operations) {
    List<RmiIdlOperation> mapped = new ArrayList<>();
    for (RmiJavaOperation operation : operations) {
      mapped.add(
          new RmiIdlOperation(
              operation.name(),
              mapType(operation.returnType()),
              mapParameters(operation.parameters()),
              mapExceptions(operation.exceptions())));
    }
    return mapped;
  }

  private static List<RmiIdlParameter> mapParameters(List<RmiJavaParameter> parameters) {
    List<RmiIdlParameter> mapped = new ArrayList<>();
    for (RmiJavaParameter parameter : parameters) {
      mapped.add(new RmiIdlParameter(parameter.name(), mapType(parameter.type())));
    }
    return mapped;
  }

  private static List<RmiIdlExceptionReference> mapExceptions(
      List<RmiJavaTypeReference> exceptions) {
    List<RmiIdlExceptionReference> mapped = new ArrayList<>();
    for (RmiJavaTypeReference exception : exceptions) {
      if (!REMOTE_EXCEPTION.equals(exception.name())) {
        mapped.add(new RmiIdlExceptionReference(exception.name(), scopedName(exception.name())));
      }
    }
    return mapped;
  }

  private static RmiIdlTypeReference mapType(RmiJavaTypeReference type) {
    return switch (type.kind()) {
      case VOID -> RmiIdlTypeReference.voidType();
      case PRIMITIVE -> RmiIdlTypeReference.builtin(mapPrimitive(type.name()));
      case DECLARED -> mapDeclaredType(type.name());
      case ARRAY -> RmiIdlTypeReference.sequenceOf(mapType(type.componentType().orElseThrow()));
      case GENERIC, WILDCARD ->
          throw new IllegalArgumentException("Unsupported type should have diagnostics first");
    };
  }

  private static RmiIdlTypeReference mapDeclaredType(String javaBinaryName) {
    if ("java.lang.String".equals(javaBinaryName)) {
      return RmiIdlTypeReference.builtin("wstring");
    }
    return RmiIdlTypeReference.declaredValue(scopedName(javaBinaryName), javaBinaryName);
  }

  private static String mapPrimitive(String javaPrimitiveName) {
    return switch (javaPrimitiveName) {
      case "boolean" -> "boolean";
      case "byte" -> "octet";
      case "char" -> "wchar";
      case "double" -> "double";
      case "float" -> "float";
      case "int" -> "long";
      case "long" -> "long long";
      case "short" -> "short";
      default -> throw new IllegalArgumentException("Unsupported primitive: " + javaPrimitiveName);
    };
  }

  private static String scopedName(String javaBinaryName) {
    return scopedName(List.of(javaBinaryName.split("\\.")));
  }

  private static String scopedName(List<String> parts) {
    return "::" + String.join("::", parts);
  }

  private static boolean hasErrors(List<Diagnostic> diagnostics) {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }

  private static void emit(List<Diagnostic> diagnostics, DiagnosticCode code, String message) {
    diagnostics.add(Diagnostic.withoutSpan(code, DiagnosticSeverity.ERROR, message));
  }

  private static boolean isIdlIdentifier(String value) {
    Objects.requireNonNull(value, "value");
    return IDL_IDENTIFIER_PATTERN.matcher(value).matches()
        && !IDL_RESERVED_WORDS.contains(value)
        && !IDL_RESERVED_WORDS.contains(value.toLowerCase(Locale.ROOT));
  }
}
