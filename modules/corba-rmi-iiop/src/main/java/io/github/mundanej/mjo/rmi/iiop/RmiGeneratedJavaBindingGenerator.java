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
    List<RmiIdlTypeReference> typeReferences = descriptorTypeReferences(idlInterface);
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
        .append(
            "  public static final io.github.mundanej.mjo.repositoryid.RepositoryId REPOSITORY_ID_VALUE =\n")
        .append("      io.github.mundanej.mjo.repositoryid.RepositoryId.parse(REPOSITORY_ID);\n");
    for (RmiIdlTypeReference type : typeReferences) {
      renderTypeReferenceConstant(source, type, context);
    }
    for (RmiIdlExceptionReference exception : collectExceptions(List.of(idlInterface))) {
      renderExceptionTypeReferenceConstant(source, exception, context);
    }
    for (RmiIdlOperation operation : idlInterface.operations()) {
      renderRmiOperationConstant(source, operation);
      renderOperationDescriptorConstant(source, operation);
    }
    source
        .append("\n")
        .append(
            "  public static final io.github.mundanej.mjo.rmi.iiop.RmiIdlInterface RMI_INTERFACE =\n")
        .append("      new io.github.mundanej.mjo.rmi.iiop.RmiIdlInterface(\n")
        .append("          \"")
        .append(idlInterface.name())
        .append("\",\n")
        .append("          \"")
        .append(idlInterface.scopedName())
        .append("\",\n")
        .append("          java.util.Optional.of(JAVA_BINARY_NAME),\n")
        .append("          java.util.List.of(")
        .append(
            idlInterface.operations().stream()
                .map(operation -> operationConstantName(operation) + "_RMI_MODEL")
                .collect(Collectors.joining(", ")))
        .append("));\n\n")
        .append(
            "  public static final io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor DESCRIPTOR =\n")
        .append("      new io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor(\n")
        .append("          io.github.mundanej.mjo.typecode.IdlTypeKind.INTERFACE,\n")
        .append("          IDL_SCOPED_NAME,\n")
        .append("          JAVA_BINARY_NAME,\n")
        .append("          REPOSITORY_ID_VALUE,\n")
        .append("          java.util.List.of(),\n")
        .append("          java.util.List.of(),\n")
        .append("          java.util.List.of(")
        .append(
            idlInterface.operations().stream()
                .map(RmiGeneratedJavaBindingGenerator::operationConstantName)
                .collect(Collectors.joining(", ")))
        .append("));\n\n")
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
        .append("  private final io.github.mundanej.mjo.orb.LocalOrb orb;\n")
        .append("  private final io.github.mundanej.mjo.orb.LocalObjectReference<")
        .append(idlInterface.name())
        .append("> reference;\n")
        .append("  private final io.github.mundanej.mjo.rmi.iiop.RmiIiopWireClient wireClient;\n")
        .append("  private final io.github.mundanej.mjo.rmi.iiop.RmiIiopObjectKey objectKey;\n\n")
        .append("  public ")
        .append(simpleName)
        .append("(io.github.mundanej.mjo.orb.LocalOrb orb,\n")
        .append("      io.github.mundanej.mjo.orb.LocalObjectReference<")
        .append(idlInterface.name())
        .append("> reference) {\n")
        .append("    this.orb = java.util.Objects.requireNonNull(orb, \"orb\");\n")
        .append(
            "    this.reference = java.util.Objects.requireNonNull(reference, \"reference\");\n")
        .append("    this.wireClient = null;\n")
        .append("    this.objectKey = null;\n")
        .append("  }\n\n")
        .append("  public ")
        .append(simpleName)
        .append("(io.github.mundanej.mjo.rmi.iiop.RmiIiopWireClient wireClient,\n")
        .append("      io.github.mundanej.mjo.rmi.iiop.RmiIiopObjectKey objectKey) {\n")
        .append("    this.orb = null;\n")
        .append("    this.reference = null;\n")
        .append(
            "    this.wireClient = java.util.Objects.requireNonNull(wireClient, \"wireClient\");\n")
        .append(
            "    this.objectKey = java.util.Objects.requireNonNull(objectKey, \"objectKey\");\n")
        .append("  }\n");
    for (RmiIdlOperation operation : idlInterface.operations()) {
      renderMethodSignature(source, operation, " {\n", true);
      renderStubMethodBody(source, idlInterface, operation);
    }
    source
        .append("\n")
        .append("  private static java.rmi.RemoteException remoteFailure(Exception exception) {\n")
        .append(
            "    return new java.rmi.RemoteException(\"RMI-IIOP invocation failed\", exception);\n")
        .append("  }\n");
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
        .append("  }\n\n")
        .append("  public io.github.mundanej.mjo.orb.LocalObjectReference<")
        .append(idlInterface.name())
        .append("> activate(io.github.mundanej.mjo.poa.Poa poa) {\n")
        .append("    return java.util.Objects.requireNonNull(poa, \"poa\")\n")
        .append("        .activateServant(\n")
        .append("            ")
        .append(idlInterface.name())
        .append(".class,\n")
        .append("            ")
        .append(idlInterface.name())
        .append("BindingDescriptor.DESCRIPTOR,\n")
        .append("            servant,\n")
        .append("            this::invoke);\n")
        .append("  }\n\n")
        .append(
            "  private Object invoke("
                + idlInterface.name()
                + " target, io.github.mundanej.mjo.modern.LocalInvocationRequest request)\n")
        .append("      throws Exception {\n");
    for (RmiIdlOperation operation : idlInterface.operations()) {
      renderTieDispatch(source, idlInterface, operation);
    }
    source
        .append("    throw new org.omg.CORBA.BAD_OPERATION(\n")
        .append(
            "        \"Unsupported local RMI-IIOP operation: \" + request.operation().name(),\n")
        .append("        0,\n")
        .append("        org.omg.CORBA.CompletionStatus.COMPLETED_NO);\n")
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
        .append("  public io.github.mundanej.mjo.orb.LocalObjectReference<")
        .append(idlInterface.name())
        .append("> activate(io.github.mundanej.mjo.poa.Poa poa) {\n")
        .append("    return new ")
        .append(idlInterface.name())
        .append("Tie(this).activate(poa);\n")
        .append("  }\n")
        .append("}\n");
    return new RmiGeneratedJavaBindingSource(packageName, simpleName, source.toString());
  }

  private static void renderStubMethodBody(
      StringBuilder source, RmiIdlInterface idlInterface, RmiIdlOperation operation) {
    source.append("    if (wireClient != null) {\n");
    renderWireStubMethodBody(source, idlInterface, operation);
    source.append("    }\n");
    source.append("    try {\n");
    String invocation =
        "orb.invoke(reference, "
            + idlInterface.name()
            + "BindingDescriptor."
            + operationConstantName(operation)
            + ", "
            + argumentList(operation)
            + ")";
    if (operation.returnType().kind() == RmiIdlTypeKind.VOID) {
      source.append("      ").append(invocation).append(";\n");
    } else {
      source.append("      Object result = ").append(invocation).append(";\n");
      source
          .append("      return ")
          .append(returnExpression(operation.returnType(), "result"))
          .append(";\n");
    }
    source.append(
        "    } catch (io.github.mundanej.mjo.orb.LocalInvocationUserException exception) {\n");
    for (RmiIdlExceptionReference exception : operation.exceptions()) {
      String simpleName = simpleName(exception.scopedName());
      source
          .append("      if (exception.userException() instanceof ")
          .append(simpleName)
          .append(" typedException) {\n")
          .append("        throw typedException;\n")
          .append("      }\n");
    }
    source
        .append("      throw remoteFailure(exception);\n")
        .append("    } catch (org.omg.CORBA.SystemException exception) {\n")
        .append("      throw remoteFailure(exception);\n")
        .append("    } catch (RuntimeException exception) {\n")
        .append("      throw remoteFailure(exception);\n")
        .append("    }\n")
        .append("  }\n");
  }

  private static void renderWireStubMethodBody(
      StringBuilder source, RmiIdlInterface idlInterface, RmiIdlOperation operation) {
    String invocation =
        "wireClient.invoke(objectKey, "
            + idlInterface.name()
            + "BindingDescriptor."
            + operationConstantName(operation)
            + "_RMI_MODEL, "
            + wireArgumentList(operation)
            + ")";
    source.append("      try {\n");
    if (operation.returnType().kind() == RmiIdlTypeKind.VOID) {
      source.append("        ").append(invocation).append(";\n");
      source.append("        return;\n");
    } else {
      source
          .append("        io.github.mundanej.mjo.rmi.iiop.RmiCdrValue result = ")
          .append(invocation)
          .append(";\n");
      source
          .append("        return ")
          .append(returnExpression(operation.returnType(), "result.value()"))
          .append(";\n");
    }
    source.append(
        "      } catch (io.github.mundanej.mjo.rmi.iiop.RmiIiopWireUserException exception) {\n");
    for (RmiIdlExceptionReference exception : operation.exceptions()) {
      String simpleName = simpleName(exception.scopedName());
      source
          .append("        if (")
          .append(simpleName)
          .append(".REPOSITORY_ID.equals(exception.repositoryId())) {\n")
          .append("          throw new ")
          .append(simpleName)
          .append("(exception.getMessage());\n")
          .append("        }\n");
    }
    source
        .append("        throw remoteFailure(exception);\n")
        .append(
            "      } catch (io.github.mundanej.mjo.rmi.iiop.RmiIiopWireException exception) {\n")
        .append("        throw remoteFailure(exception);\n")
        .append("      }\n");
  }

  private static void renderTieDispatch(
      StringBuilder source, RmiIdlInterface idlInterface, RmiIdlOperation operation) {
    source
        .append("    if (")
        .append(idlInterface.name())
        .append("BindingDescriptor.")
        .append(operationConstantName(operation))
        .append(".equals(request.operation())) {\n");
    if (operation.returnType().kind() != RmiIdlTypeKind.VOID) {
      source.append("      return ");
    } else {
      source.append("      ");
    }
    source
        .append("target.")
        .append(operation.name())
        .append('(')
        .append(requestArguments(operation))
        .append(");\n");
    if (operation.returnType().kind() == RmiIdlTypeKind.VOID) {
      source.append("      return null;\n");
    }
    source.append("    }\n");
  }

  private static List<RmiIdlTypeReference> descriptorTypeReferences(RmiIdlInterface idlInterface) {
    Map<String, RmiIdlTypeReference> types = new LinkedHashMap<>();
    for (RmiIdlOperation operation : idlInterface.operations()) {
      types.putIfAbsent(typeConstantName(operation.returnType()), operation.returnType());
      for (RmiIdlParameter parameter : operation.parameters()) {
        types.putIfAbsent(typeConstantName(parameter.type()), parameter.type());
      }
    }
    return List.copyOf(types.values());
  }

  private static void renderTypeReferenceConstant(
      StringBuilder source, RmiIdlTypeReference type, BindingContext context) {
    source
        .append("\n")
        .append("  public static final io.github.mundanej.mjo.typecode.IdlTypeReference ")
        .append(typeConstantName(type))
        .append(" =\n")
        .append("      new io.github.mundanej.mjo.typecode.IdlTypeReference(\n")
        .append("          ")
        .append(idlTypeKindExpression(type))
        .append(",\n")
        .append("          \"")
        .append(type.name())
        .append("\",\n")
        .append("          \"")
        .append(typeJavaName(type))
        .append("\",\n")
        .append(repositoryIdExpression(type, context))
        .append(");\n");
  }

  private static void renderExceptionTypeReferenceConstant(
      StringBuilder source, RmiIdlExceptionReference exception, BindingContext context) {
    source
        .append("\n")
        .append("  public static final io.github.mundanej.mjo.typecode.IdlTypeReference ")
        .append(exceptionTypeConstantName(exception))
        .append(" =\n")
        .append("      new io.github.mundanej.mjo.typecode.IdlTypeReference(\n")
        .append("          io.github.mundanej.mjo.typecode.IdlTypeKind.EXCEPTION,\n")
        .append("          \"")
        .append(exception.scopedName())
        .append("\",\n")
        .append("          \"")
        .append(javaName(exception.scopedName()))
        .append("\",\n")
        .append("          java.util.Optional.of(\n")
        .append("              io.github.mundanej.mjo.repositoryid.RepositoryId.parse(\"")
        .append(context.repositoryIds().get(exception.javaBinaryName()))
        .append("\")));\n");
  }

  private static void renderRmiOperationConstant(StringBuilder source, RmiIdlOperation operation) {
    source
        .append("\n")
        .append("  public static final io.github.mundanej.mjo.rmi.iiop.RmiIdlOperation ")
        .append(operationConstantName(operation))
        .append("_RMI_MODEL =\n")
        .append("      new io.github.mundanej.mjo.rmi.iiop.RmiIdlOperation(\n")
        .append("          \"")
        .append(operation.name())
        .append("\",\n")
        .append("          ")
        .append(rmiTypeReferenceExpression(operation.returnType()))
        .append(",\n")
        .append("          java.util.List.of(")
        .append(
            operation.parameters().stream()
                .map(RmiGeneratedJavaBindingGenerator::rmiParameterExpression)
                .collect(Collectors.joining(", ")))
        .append("),\n")
        .append("          java.util.List.of(")
        .append(
            operation.exceptions().stream()
                .map(RmiGeneratedJavaBindingGenerator::rmiExceptionExpression)
                .collect(Collectors.joining(", ")))
        .append("));\n");
  }

  private static void renderOperationDescriptorConstant(
      StringBuilder source, RmiIdlOperation operation) {
    source
        .append("\n")
        .append("  public static final io.github.mundanej.mjo.typecode.IdlOperationDescriptor ")
        .append(operationConstantName(operation))
        .append(" =\n")
        .append("      new io.github.mundanej.mjo.typecode.IdlOperationDescriptor(\n")
        .append("          \"")
        .append(operation.name())
        .append("\",\n")
        .append("          ")
        .append(typeConstantName(operation.returnType()))
        .append(",\n")
        .append("          java.util.List.of(")
        .append(
            operation.parameters().stream()
                .map(RmiGeneratedJavaBindingGenerator::parameterDescriptorExpression)
                .collect(Collectors.joining(", ")))
        .append("),\n")
        .append("          java.util.List.of(")
        .append(
            operation.exceptions().stream()
                .map(RmiGeneratedJavaBindingGenerator::exceptionTypeConstantName)
                .collect(Collectors.joining(", ")))
        .append("));\n");
  }

  private static StringBuilder header(String packageName) {
    StringBuilder source = new StringBuilder();
    source
        .append("// Generated by mundane-java-orb G7-080.\n")
        .append("// Compatibility profile: local and wire RMI-IIOP binding surface.\n\n");
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

  private static String parameterDescriptorExpression(RmiIdlParameter parameter) {
    return "new io.github.mundanej.mjo.typecode.IdlParameterDescriptor(\""
        + parameter.name()
        + "\", io.github.mundanej.mjo.typecode.IdlParameterMode.IN, "
        + typeConstantName(parameter.type())
        + ")";
  }

  private static String argumentList(RmiIdlOperation operation) {
    if (operation.parameters().isEmpty()) {
      return "java.util.List.of()";
    }
    return operation.parameters().stream()
        .map(RmiIdlParameter::name)
        .collect(Collectors.joining(", ", "java.util.List.of(", ")"));
  }

  private static String wireArgumentList(RmiIdlOperation operation) {
    if (operation.parameters().isEmpty()) {
      return "java.util.List.of()";
    }
    return operation.parameters().stream()
        .map(parameter -> wireValueExpression(parameter.type(), parameter.name()))
        .collect(Collectors.joining(", ", "java.util.List.of(", ")"));
  }

  private static String wireValueExpression(RmiIdlTypeReference type, String valueExpression) {
    if (type.kind() == RmiIdlTypeKind.BUILTIN) {
      return switch (type.name()) {
        case "boolean" ->
            "io.github.mundanej.mjo.rmi.iiop.RmiCdrValue.booleanValue(" + valueExpression + ")";
        case "octet" ->
            "io.github.mundanej.mjo.rmi.iiop.RmiCdrValue.octetValue(" + valueExpression + ")";
        case "wchar" ->
            "io.github.mundanej.mjo.rmi.iiop.RmiCdrValue.wcharValue(" + valueExpression + ")";
        case "double" ->
            "io.github.mundanej.mjo.rmi.iiop.RmiCdrValue.doubleValue(" + valueExpression + ")";
        case "float" ->
            "io.github.mundanej.mjo.rmi.iiop.RmiCdrValue.floatValue(" + valueExpression + ")";
        case "long" ->
            "io.github.mundanej.mjo.rmi.iiop.RmiCdrValue.longValue(" + valueExpression + ")";
        case "long long" ->
            "io.github.mundanej.mjo.rmi.iiop.RmiCdrValue.longLongValue(" + valueExpression + ")";
        case "short" ->
            "io.github.mundanej.mjo.rmi.iiop.RmiCdrValue.shortValue(" + valueExpression + ")";
        case "wstring" ->
            "io.github.mundanej.mjo.rmi.iiop.RmiCdrValue.stringValue(" + valueExpression + ")";
        default ->
            "new io.github.mundanej.mjo.rmi.iiop.RmiCdrValue("
                + rmiTypeReferenceExpression(type)
                + ", "
                + valueExpression
                + ")";
      };
    }
    return "new io.github.mundanej.mjo.rmi.iiop.RmiCdrValue("
        + rmiTypeReferenceExpression(type)
        + ", "
        + valueExpression
        + ")";
  }

  private static String requestArguments(RmiIdlOperation operation) {
    List<String> arguments = new ArrayList<>();
    for (int index = 0; index < operation.parameters().size(); index++) {
      RmiIdlParameter parameter = operation.parameters().get(index);
      arguments.add(castExpression(parameter.type(), "request.arguments().get(" + index + ")"));
    }
    return String.join(", ", arguments);
  }

  private static String returnExpression(RmiIdlTypeReference type, String valueExpression) {
    return castExpression(type, valueExpression);
  }

  private static String castExpression(RmiIdlTypeReference type, String valueExpression) {
    if (type.kind() == RmiIdlTypeKind.BUILTIN) {
      return switch (type.name()) {
        case "boolean" -> "((java.lang.Boolean) " + valueExpression + ").booleanValue()";
        case "octet" -> "((java.lang.Byte) " + valueExpression + ").byteValue()";
        case "wchar" -> "((java.lang.Character) " + valueExpression + ").charValue()";
        case "double" -> "((java.lang.Double) " + valueExpression + ").doubleValue()";
        case "float" -> "((java.lang.Float) " + valueExpression + ").floatValue()";
        case "long" -> "((java.lang.Integer) " + valueExpression + ").intValue()";
        case "long long" -> "((java.lang.Long) " + valueExpression + ").longValue()";
        case "short" -> "((java.lang.Short) " + valueExpression + ").shortValue()";
        case "wstring" -> "(java.lang.String) " + valueExpression;
        default ->
            throw new IllegalArgumentException("Unsupported IDL built-in type: " + type.name());
      };
    }
    if (type.kind() == RmiIdlTypeKind.DECLARED_VALUE) {
      return "(" + javaType(type) + ") " + valueExpression;
    }
    throw new IllegalArgumentException("Unsupported cast type: " + type.kind());
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

  private static String idlTypeKindExpression(RmiIdlTypeReference type) {
    return switch (type.kind()) {
      case VOID -> "io.github.mundanej.mjo.typecode.IdlTypeKind.VOID";
      case BUILTIN -> "io.github.mundanej.mjo.typecode.IdlTypeKind.PRIMITIVE";
      case DECLARED_VALUE -> "io.github.mundanej.mjo.typecode.IdlTypeKind.INTERFACE";
      case SEQUENCE -> throw new IllegalArgumentException("Sequence type should have diagnostics");
    };
  }

  private static String typeJavaName(RmiIdlTypeReference type) {
    return switch (type.kind()) {
      case VOID -> "void";
      case BUILTIN -> javaType(type);
      case DECLARED_VALUE -> javaName(type.name());
      case SEQUENCE -> throw new IllegalArgumentException("Sequence type should have diagnostics");
    };
  }

  private static String rmiParameterExpression(RmiIdlParameter parameter) {
    return "new io.github.mundanej.mjo.rmi.iiop.RmiIdlParameter(\""
        + parameter.name()
        + "\", "
        + rmiTypeReferenceExpression(parameter.type())
        + ")";
  }

  private static String rmiExceptionExpression(RmiIdlExceptionReference exception) {
    return "new io.github.mundanej.mjo.rmi.iiop.RmiIdlExceptionReference(\""
        + exception.javaBinaryName()
        + "\", \""
        + exception.scopedName()
        + "\")";
  }

  private static String rmiTypeReferenceExpression(RmiIdlTypeReference type) {
    return switch (type.kind()) {
      case VOID -> "io.github.mundanej.mjo.rmi.iiop.RmiIdlTypeReference.voidType()";
      case BUILTIN ->
          "io.github.mundanej.mjo.rmi.iiop.RmiIdlTypeReference.builtin(\"" + type.name() + "\")";
      case DECLARED_VALUE ->
          "io.github.mundanej.mjo.rmi.iiop.RmiIdlTypeReference.declaredValue(\""
              + type.name()
              + "\", \""
              + type.javaBinaryName().orElseThrow()
              + "\")";
      case SEQUENCE -> throw new IllegalArgumentException("Sequence type should have diagnostics");
    };
  }

  private static String repositoryIdExpression(RmiIdlTypeReference type, BindingContext context) {
    if (type.kind() != RmiIdlTypeKind.DECLARED_VALUE) {
      return "          java.util.Optional.empty()";
    }
    return "          java.util.Optional.of(\n"
        + "              io.github.mundanej.mjo.repositoryid.RepositoryId.parse(\""
        + context.repositoryIds().get(type.javaBinaryName().orElseThrow())
        + "\"))";
  }

  private static String typeConstantName(RmiIdlTypeReference type) {
    return switch (type.kind()) {
      case VOID -> "VOID_TYPE";
      case BUILTIN -> upperSnake(type.name()) + "_TYPE";
      case DECLARED_VALUE -> upperSnake(simpleName(type.name())) + "_TYPE";
      case SEQUENCE -> throw new IllegalArgumentException("Sequence type should have diagnostics");
    };
  }

  private static String exceptionTypeConstantName(RmiIdlExceptionReference exception) {
    return upperSnake(simpleName(exception.scopedName())) + "_EXCEPTION_TYPE";
  }

  private static String operationConstantName(RmiIdlOperation operation) {
    return upperSnake(operation.name()) + "_OPERATION";
  }

  private static String javaName(String scopedName) {
    return String.join(".", scopedParts(scopedName));
  }

  private static String upperSnake(String value) {
    StringBuilder output = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if (Character.isLetterOrDigit(character)) {
        if (Character.isUpperCase(character) && index > 0 && output.length() > 0) {
          output.append('_');
        }
        output.append(Character.toUpperCase(character));
      } else if (output.length() > 0 && output.charAt(output.length() - 1) != '_') {
        output.append('_');
      }
    }
    if (output.length() > 0 && output.charAt(output.length() - 1) == '_') {
      output.deleteCharAt(output.length() - 1);
    }
    return output.toString();
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
