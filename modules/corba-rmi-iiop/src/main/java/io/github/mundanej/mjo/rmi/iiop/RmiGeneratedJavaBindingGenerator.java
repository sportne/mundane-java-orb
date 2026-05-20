package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Generates deterministic compile-safe Java binding surfaces from RMI Java-to-IDL models. */
public final class RmiGeneratedJavaBindingGenerator {

  private static final String DEFERRED_MESSAGE =
      "RMI-IIOP invocation is deferred to G7-070/G7-080.";

  /** Creates a stateless generated Java binding generator. */
  public RmiGeneratedJavaBindingGenerator() {}

  /** Generates compile-safe Java binding source values for the supplied model and IDs. */
  public RmiGeneratedJavaBindingResult generate(
      RmiIdlTranslationUnit translationUnit, RmiRepositoryIdPlan repositoryIdPlan) {
    Objects.requireNonNull(translationUnit, "translationUnit");
    Objects.requireNonNull(repositoryIdPlan, "repositoryIdPlan");
    BindingContext context = BindingContext.from(translationUnit, repositoryIdPlan);
    List<Diagnostic> diagnostics = new ArrayList<>();
    validateTranslationUnit(translationUnit, List.of(), context, diagnostics);
    if (hasErrors(diagnostics)) {
      return new RmiGeneratedJavaBindingResult(List.of(), diagnostics);
    }

    List<RmiGeneratedJavaBindingSource> sources = new ArrayList<>();
    renderTranslationUnit(translationUnit, List.of(), context, sources);
    List<RmiGeneratedJavaBindingSource> sorted =
        sources.stream()
            .sorted(Comparator.comparing(RmiGeneratedJavaBindingSource::sourcePath))
            .toList();
    assertUniqueSourcePaths(sorted, diagnostics);
    if (hasErrors(diagnostics)) {
      return new RmiGeneratedJavaBindingResult(List.of(), diagnostics);
    }
    return new RmiGeneratedJavaBindingResult(sorted, diagnostics);
  }

  private record BindingContext(Map<String, String> repositoryIds, Set<String> interfaceNames) {

    private static BindingContext from(
        RmiIdlTranslationUnit translationUnit, RmiRepositoryIdPlan repositoryIdPlan) {
      Map<String, String> repositoryIds = new LinkedHashMap<>();
      for (RmiRepositoryIdValue value : repositoryIdPlan.repositoryIds()) {
        repositoryIds.put(value.javaBinaryName(), value.repositoryId());
      }
      Set<String> interfaceNames = new LinkedHashSet<>();
      collectInterfaceNames(translationUnit, interfaceNames);
      return new BindingContext(Map.copyOf(repositoryIds), Set.copyOf(interfaceNames));
    }

    private static void collectInterfaceNames(
        RmiIdlTranslationUnit translationUnit, Set<String> interfaceNames) {
      for (RmiIdlInterface idlInterface : translationUnit.interfaces()) {
        interfaceNames.add(idlInterface.scopedName());
      }
      for (RmiIdlModule module : translationUnit.modules()) {
        collectInterfaceNames(module, interfaceNames);
      }
    }

    private static void collectInterfaceNames(RmiIdlModule module, Set<String> interfaceNames) {
      for (RmiIdlInterface idlInterface : module.interfaces()) {
        interfaceNames.add(idlInterface.scopedName());
      }
      for (RmiIdlModule nestedModule : module.modules()) {
        collectInterfaceNames(nestedModule, interfaceNames);
      }
    }
  }

  private static void validateTranslationUnit(
      RmiIdlTranslationUnit translationUnit,
      List<String> modulePath,
      BindingContext context,
      List<Diagnostic> diagnostics) {
    validateInterfaces(translationUnit.interfaces(), modulePath, context, diagnostics);
    for (RmiIdlModule module : translationUnit.modules()) {
      List<String> nestedPath = new ArrayList<>(modulePath);
      nestedPath.add(module.name());
      validateModule(module, nestedPath, context, diagnostics);
    }
  }

  private static void validateModule(
      RmiIdlModule module,
      List<String> modulePath,
      BindingContext context,
      List<Diagnostic> diagnostics) {
    validateInterfaces(module.interfaces(), modulePath, context, diagnostics);
    for (RmiIdlModule nestedModule : module.modules()) {
      List<String> nestedPath = new ArrayList<>(modulePath);
      nestedPath.add(nestedModule.name());
      validateModule(nestedModule, nestedPath, context, diagnostics);
    }
  }

  private static void validateInterfaces(
      List<RmiIdlInterface> interfaces,
      List<String> modulePath,
      BindingContext context,
      List<Diagnostic> diagnostics) {
    for (RmiIdlInterface idlInterface : interfaces) {
      if (idlInterface.javaBinaryName().isEmpty()
          || !context.repositoryIds().containsKey(idlInterface.javaBinaryName().orElse(""))) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.MISSING_BINDING_REPOSITORY_ID,
            "Missing generated binding repository ID for " + idlInterface.scopedName());
      }
      for (RmiIdlOperation operation : idlInterface.operations()) {
        validateType(operation.returnType(), context, operation.name(), diagnostics);
        for (RmiIdlParameter parameter : operation.parameters()) {
          validateType(
              parameter.type(), context, operation.name() + "." + parameter.name(), diagnostics);
        }
        for (RmiIdlExceptionReference exception : operation.exceptions()) {
          validateException(exception, modulePath, context, diagnostics);
        }
      }
    }
  }

  private static void validateException(
      RmiIdlExceptionReference exception,
      List<String> modulePath,
      BindingContext context,
      List<Diagnostic> diagnostics) {
    List<String> exceptionParts = scopedParts(exception.scopedName());
    if (exceptionParts.size() != modulePath.size() + 1
        || !exceptionParts.subList(0, modulePath.size()).equals(modulePath)) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_EXCEPTION_SCOPE,
          "Generated Java bindings require same-module exceptions: " + exception.scopedName());
    }
    if (!context.repositoryIds().containsKey(exception.javaBinaryName())) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.MISSING_BINDING_REPOSITORY_ID,
          "Missing generated binding repository ID for " + exception.javaBinaryName());
    }
  }

  private static void validateType(
      RmiIdlTypeReference type,
      BindingContext context,
      String location,
      List<Diagnostic> diagnostics) {
    if (type.kind() == RmiIdlTypeKind.SEQUENCE) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_SEQUENCE,
          "Generated Java bindings do not emit sequence types in G7-050: " + location);
      return;
    }
    if (type.kind() == RmiIdlTypeKind.DECLARED_VALUE
        && !context.interfaceNames().contains(type.name())) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_DECLARED_TYPE,
          "Generated Java bindings do not emit undeclared value/reference types in G7-050: "
              + location);
    }
  }

  private static void renderTranslationUnit(
      RmiIdlTranslationUnit translationUnit,
      List<String> modulePath,
      BindingContext context,
      List<RmiGeneratedJavaBindingSource> sources) {
    renderInterfaces(translationUnit.interfaces(), modulePath, context, sources);
    for (RmiIdlModule module : translationUnit.modules()) {
      List<String> nestedPath = new ArrayList<>(modulePath);
      nestedPath.add(module.name());
      renderModule(module, nestedPath, context, sources);
    }
  }

  private static void renderModule(
      RmiIdlModule module,
      List<String> modulePath,
      BindingContext context,
      List<RmiGeneratedJavaBindingSource> sources) {
    renderInterfaces(module.interfaces(), modulePath, context, sources);
    for (RmiIdlModule nestedModule : module.modules()) {
      List<String> nestedPath = new ArrayList<>(modulePath);
      nestedPath.add(nestedModule.name());
      renderModule(nestedModule, nestedPath, context, sources);
    }
  }

  private static void renderInterfaces(
      List<RmiIdlInterface> interfaces,
      List<String> modulePath,
      BindingContext context,
      List<RmiGeneratedJavaBindingSource> sources) {
    String packageName = packageName(modulePath);
    Set<RmiIdlExceptionReference> exceptions = collectExceptions(interfaces);
    for (RmiIdlExceptionReference exception : exceptions) {
      sources.add(renderException(packageName, exception, context));
      sources.add(renderExceptionHolder(packageName, exception));
    }
    for (RmiIdlInterface idlInterface : interfaces) {
      sources.add(renderRemoteInterface(packageName, idlInterface));
      sources.add(renderHelper(packageName, idlInterface, context));
      sources.add(renderHolder(packageName, idlInterface));
      sources.add(renderDescriptor(packageName, idlInterface, context));
      sources.add(renderStub(packageName, idlInterface));
      sources.add(renderTie(packageName, idlInterface));
      sources.add(renderSkeleton(packageName, idlInterface));
    }
  }

  private static Set<RmiIdlExceptionReference> collectExceptions(List<RmiIdlInterface> interfaces) {
    Set<RmiIdlExceptionReference> exceptions = new LinkedHashSet<>();
    for (RmiIdlInterface idlInterface : interfaces) {
      for (RmiIdlOperation operation : idlInterface.operations()) {
        exceptions.addAll(operation.exceptions());
      }
    }
    return exceptions;
  }

  private static RmiGeneratedJavaBindingSource renderRemoteInterface(
      String packageName, RmiIdlInterface idlInterface) {
    String simpleName = idlInterface.name();
    StringBuilder source = header(packageName);
    source.append("public interface ").append(simpleName).append(" extends java.rmi.Remote {\n");
    for (RmiIdlOperation operation : idlInterface.operations()) {
      renderMethodSignature(source, operation, ";\n", false);
    }
    source.append("}\n");
    return new RmiGeneratedJavaBindingSource(packageName, simpleName, source.toString());
  }

  private static RmiGeneratedJavaBindingSource renderException(
      String packageName, RmiIdlExceptionReference exception, BindingContext context) {
    String simpleName = simpleName(exception.scopedName());
    StringBuilder source = header(packageName);
    source
        .append("public class ")
        .append(simpleName)
        .append(" extends java.lang.Exception {\n\n")
        .append("  private static final long serialVersionUID = 1L;\n\n")
        .append("  public static final String REPOSITORY_ID = \"")
        .append(context.repositoryIds().get(exception.javaBinaryName()))
        .append("\";\n\n")
        .append("  public ")
        .append(simpleName)
        .append("() {\n")
        .append("    super();\n")
        .append("  }\n\n")
        .append("  public ")
        .append(simpleName)
        .append("(String message) {\n")
        .append("    super(message);\n")
        .append("  }\n")
        .append("}\n");
    return new RmiGeneratedJavaBindingSource(packageName, simpleName, source.toString());
  }

  private static RmiGeneratedJavaBindingSource renderHelper(
      String packageName, RmiIdlInterface idlInterface, BindingContext context) {
    String simpleName = idlInterface.name() + "Helper";
    StringBuilder source = header(packageName);
    source
        .append("public final class ")
        .append(simpleName)
        .append(" {\n\n")
        .append("  public static final String ID = \"")
        .append(context.repositoryIds().get(idlInterface.javaBinaryName().orElseThrow()))
        .append("\";\n\n")
        .append("  private ")
        .append(simpleName)
        .append("() {}\n\n")
        .append("  public static String id() {\n")
        .append("    return ID;\n")
        .append("  }\n")
        .append("}\n");
    return new RmiGeneratedJavaBindingSource(packageName, simpleName, source.toString());
  }

  private static RmiGeneratedJavaBindingSource renderHolder(
      String packageName, RmiIdlInterface idlInterface) {
    return renderHolder(packageName, idlInterface.name() + "Holder", idlInterface.name());
  }

  private static RmiGeneratedJavaBindingSource renderExceptionHolder(
      String packageName, RmiIdlExceptionReference exception) {
    String exceptionName = simpleName(exception.scopedName());
    return renderHolder(packageName, exceptionName + "Holder", exceptionName);
  }

  private static RmiGeneratedJavaBindingSource renderHolder(
      String packageName, String simpleName, String valueType) {
    StringBuilder source = header(packageName);
    source
        .append("public final class ")
        .append(simpleName)
        .append(" {\n\n")
        .append("  public ")
        .append(valueType)
        .append(" value;\n\n")
        .append("  public ")
        .append(simpleName)
        .append("() {}\n\n")
        .append("  public ")
        .append(simpleName)
        .append('(')
        .append(valueType)
        .append(" value) {\n")
        .append("    this.value = value;\n")
        .append("  }\n")
        .append("}\n");
    return new RmiGeneratedJavaBindingSource(packageName, simpleName, source.toString());
  }

  private static RmiGeneratedJavaBindingSource renderDescriptor(
      String packageName, RmiIdlInterface idlInterface, BindingContext context) {
    String simpleName = idlInterface.name() + "BindingDescriptor";
    StringBuilder source = header(packageName);
    source
        .append("public final class ")
        .append(simpleName)
        .append(" {\n\n")
        .append("  public static final String JAVA_BINARY_NAME = \"")
        .append(idlInterface.javaBinaryName().orElseThrow())
        .append("\";\n\n")
        .append("  public static final String IDL_SCOPED_NAME = \"")
        .append(idlInterface.scopedName())
        .append("\";\n\n")
        .append("  public static final String REPOSITORY_ID = \"")
        .append(context.repositoryIds().get(idlInterface.javaBinaryName().orElseThrow()))
        .append("\";\n\n")
        .append("  public static final java.util.List<String> OPERATIONS = java.util.List.of(")
        .append(
            idlInterface.operations().stream()
                .map(operation -> "\"" + operation.name() + "\"")
                .collect(Collectors.joining(", ")))
        .append(");\n\n")
        .append("  private ")
        .append(simpleName)
        .append("() {}\n")
        .append("}\n");
    return new RmiGeneratedJavaBindingSource(packageName, simpleName, source.toString());
  }

  private static RmiGeneratedJavaBindingSource renderStub(
      String packageName, RmiIdlInterface idlInterface) {
    String simpleName = idlInterface.name() + "Stub";
    StringBuilder source = header(packageName);
    source
        .append("public final class ")
        .append(simpleName)
        .append(" implements ")
        .append(idlInterface.name())
        .append(" {\n\n")
        .append("  public static final String DEFERRED_INVOCATION_MESSAGE = \"")
        .append(DEFERRED_MESSAGE)
        .append("\";\n");
    for (RmiIdlOperation operation : idlInterface.operations()) {
      renderMethodSignature(source, operation, " {\n", true);
      source
          .append("    throw new UnsupportedOperationException(DEFERRED_INVOCATION_MESSAGE);\n")
          .append("  }\n");
    }
    source.append("}\n");
    return new RmiGeneratedJavaBindingSource(packageName, simpleName, source.toString());
  }

  private static RmiGeneratedJavaBindingSource renderTie(
      String packageName, RmiIdlInterface idlInterface) {
    String simpleName = idlInterface.name() + "Tie";
    StringBuilder source = header(packageName);
    source
        .append("public final class ")
        .append(simpleName)
        .append(" {\n\n")
        .append("  private final ")
        .append(idlInterface.name())
        .append(" servant;\n\n")
        .append("  public ")
        .append(simpleName)
        .append('(')
        .append(idlInterface.name())
        .append(" servant) {\n")
        .append("    this.servant = java.util.Objects.requireNonNull(servant, \"servant\");\n")
        .append("  }\n\n")
        .append("  public ")
        .append(idlInterface.name())
        .append(" servant() {\n")
        .append("    return servant;\n")
        .append("  }\n")
        .append("}\n");
    return new RmiGeneratedJavaBindingSource(packageName, simpleName, source.toString());
  }

  private static RmiGeneratedJavaBindingSource renderSkeleton(
      String packageName, RmiIdlInterface idlInterface) {
    String simpleName = idlInterface.name() + "Skeleton";
    StringBuilder source = header(packageName);
    source
        .append("public abstract class ")
        .append(simpleName)
        .append(" implements ")
        .append(idlInterface.name())
        .append(" {\n\n")
        .append("  public static final String DEFERRED_INVOCATION_MESSAGE = \"")
        .append(DEFERRED_MESSAGE)
        .append("\";\n")
        .append("}\n");
    return new RmiGeneratedJavaBindingSource(packageName, simpleName, source.toString());
  }

  private static StringBuilder header(String packageName) {
    StringBuilder source = new StringBuilder();
    source
        .append("// Generated by mundane-java-orb G7-050.\n")
        .append("// Compatibility profile: compile-safe RMI-IIOP binding surface.\n\n");
    if (!packageName.isEmpty()) {
      source.append("package ").append(packageName).append(";\n\n");
    }
    return source;
  }

  private static void renderMethodSignature(
      StringBuilder source, RmiIdlOperation operation, String terminator, boolean override) {
    source.append('\n');
    if (override) {
      source.append("  @Override\n");
    }
    source
        .append("  public ")
        .append(javaType(operation.returnType()))
        .append(' ')
        .append(operation.name())
        .append('(')
        .append(
            operation.parameters().stream()
                .map(RmiGeneratedJavaBindingGenerator::parameter)
                .collect(Collectors.joining(", ")))
        .append(") throws ")
        .append(throwsClause(operation))
        .append(terminator);
  }

  private static String throwsClause(RmiIdlOperation operation) {
    List<String> exceptions = new ArrayList<>();
    exceptions.add("java.rmi.RemoteException");
    for (RmiIdlExceptionReference exception : operation.exceptions()) {
      exceptions.add(simpleName(exception.scopedName()));
    }
    return String.join(", ", exceptions);
  }

  private static String parameter(RmiIdlParameter parameter) {
    return javaType(parameter.type()) + " " + parameter.name();
  }

  private static String javaType(RmiIdlTypeReference type) {
    return switch (type.kind()) {
      case VOID -> "void";
      case BUILTIN -> builtinJavaType(type.name());
      case DECLARED_VALUE -> javaName(type.name());
      case SEQUENCE -> throw new IllegalArgumentException("Sequence type should have diagnostics");
    };
  }

  private static String builtinJavaType(String idlName) {
    return switch (idlName) {
      case "boolean" -> "boolean";
      case "octet" -> "byte";
      case "wchar" -> "char";
      case "double" -> "double";
      case "float" -> "float";
      case "long" -> "int";
      case "long long" -> "long";
      case "short" -> "short";
      case "wstring" -> "java.lang.String";
      default -> throw new IllegalArgumentException("Unsupported IDL built-in type: " + idlName);
    };
  }

  private static String javaName(String scopedName) {
    return String.join(".", scopedParts(scopedName));
  }

  private static void assertUniqueSourcePaths(
      List<RmiGeneratedJavaBindingSource> sources, List<Diagnostic> diagnostics) {
    Set<String> paths = new HashSet<>();
    Set<String> duplicates = new LinkedHashSet<>();
    for (RmiGeneratedJavaBindingSource source : sources) {
      if (!paths.add(source.sourcePath())) {
        duplicates.add(source.sourcePath());
      }
    }
    for (String duplicate : duplicates) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.DUPLICATE_BINDING_SOURCE_PATH,
          "Duplicate generated Java binding source path: " + duplicate);
    }
  }

  private static boolean hasErrors(List<Diagnostic> diagnostics) {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }

  private static void emit(List<Diagnostic> diagnostics, DiagnosticCode code, String message) {
    diagnostics.add(Diagnostic.withoutSpan(code, DiagnosticSeverity.ERROR, message));
  }

  private static String packageName(List<String> modulePath) {
    return String.join(".", modulePath);
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
}
