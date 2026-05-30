package io.github.mundanej.mjo.codegen;

import io.github.mundanej.mjo.idl.java.mapping.JavaMappedAttribute;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappedConstant;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappedConstantScope;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappedField;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappedOperation;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappedParameter;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappedType;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappedTypeKind;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappingMode;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappingModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Renders deterministic compile-safe Java source from a Java mapping model. */
public final class JavaSourceGenerator {

  /** Creates a stateless source generator. */
  public JavaSourceGenerator() {}

  /** Generates Java sources sorted by deterministic relative source path. */
  public List<GeneratedJavaSource> generate(JavaMappingModel model) {
    Objects.requireNonNull(model, "model");
    List<GeneratedJavaSource> sources = new ArrayList<>();
    for (JavaMappedConstantScope constantScope : model.constantScopes()) {
      sources.add(renderConstants(model, constantScope));
    }
    for (JavaMappedType type : model.types()) {
      sources.add(renderType(model, type));
      if (model.mode() == JavaMappingMode.LEGACY_COMPATIBILITY
          && type.kind() != JavaMappedTypeKind.HOLDER) {
        sources.add(renderHelper(model, type));
        sources.add(
            renderHolder(model, type, type.name().simpleName() + "Holder", holderValueType(type)));
        if (type.kind() == JavaMappedTypeKind.INTERFACE) {
          sources.add(renderStub(model, type));
          sources.add(renderPoa(model, type));
        }
      }
    }
    List<GeneratedJavaSource> sorted =
        sources.stream().sorted(Comparator.comparing(GeneratedJavaSource::sourcePath)).toList();
    assertUniqueSourcePaths(sorted);
    return sorted;
  }

  private static void assertUniqueSourcePaths(List<GeneratedJavaSource> sources) {
    Set<String> paths = new HashSet<>();
    for (GeneratedJavaSource source : sources) {
      if (!paths.add(source.sourcePath())) {
        throw new IllegalArgumentException(
            "Duplicate generated Java source path: " + source.sourcePath());
      }
    }
  }

  private GeneratedJavaSource renderConstants(
      JavaMappingModel model, JavaMappedConstantScope constantScope) {
    StringBuilder source = header(model, constantScope.name().packageName());
    source
        .append("public final class ")
        .append(constantScope.name().simpleName())
        .append(" {\n\n")
        .append("  private ")
        .append(constantScope.name().simpleName())
        .append("() {}\n\n");
    for (JavaMappedConstant constant : constantScope.constants()) {
      source
          .append("  public static final ")
          .append(constant.javaType())
          .append(' ')
          .append(constant.name())
          .append(" = ")
          .append(constant.initializer())
          .append(";\n");
    }
    source.append("}\n");
    return new GeneratedJavaSource(
        constantScope.name().packageName(), constantScope.name().simpleName(), source.toString());
  }

  private GeneratedJavaSource renderType(JavaMappingModel model, JavaMappedType type) {
    StringBuilder source = header(model, type.name().packageName());
    switch (type.kind()) {
      case INTERFACE -> renderInterface(source, type);
      case INTERFACE_FORWARD -> renderInterface(source, type);
      case STRUCT -> renderStruct(source, type);
      case ENUM -> renderEnum(source, type);
      case EXCEPTION -> renderException(source, type);
      case TYPEDEF -> renderTypedef(source, type);
      case UNION -> renderUnion(source, type);
      case NATIVE -> renderNative(source, type);
      case VALUE_BOX -> renderStruct(source, type);
      case VALUETYPE -> renderValueType(source, type);
      case HOLDER -> renderHolderClass(source, type.name().simpleName(), type.aliasType());
    }
    return new GeneratedJavaSource(
        type.name().packageName(), type.name().simpleName(), source.toString());
  }

  private GeneratedJavaSource renderHelper(JavaMappingModel model, JavaMappedType type) {
    String simpleName = type.name().simpleName() + "Helper";
    StringBuilder source = header(model, type.name().packageName());
    source
        .append("public final class ")
        .append(simpleName)
        .append(" {\n\n")
        .append("  private static final String ID = \"")
        .append(escapeJava(type.repositoryId()))
        .append("\";\n\n")
        .append("  private ")
        .append(simpleName)
        .append("() {}\n\n")
        .append("  public static String id() {\n")
        .append("    return ID;\n")
        .append("  }\n\n")
        .append("  public static String typeName() {\n")
        .append("    return \"")
        .append(escapeJava(holderValueType(type)))
        .append("\";\n")
        .append("  }\n\n")
        .append("  public static ")
        .append(holderValueType(type))
        .append(" narrow(java.lang.Object value) {\n")
        .append("    return (")
        .append(castType(holderValueType(type)))
        .append(") value;\n")
        .append("  }\n")
        .append("}\n");
    return new GeneratedJavaSource(type.name().packageName(), simpleName, source.toString());
  }

  private GeneratedJavaSource renderHolder(
      JavaMappingModel model, JavaMappedType type, String simpleName, String valueType) {
    StringBuilder source = header(model, type.name().packageName());
    renderHolderClass(source, simpleName, valueType);
    return new GeneratedJavaSource(type.name().packageName(), simpleName, source.toString());
  }

  private GeneratedJavaSource renderStub(JavaMappingModel model, JavaMappedType type) {
    String simpleName = "_" + type.name().simpleName() + "Stub";
    StringBuilder source = header(model, type.name().packageName());
    source
        .append("public abstract class ")
        .append(simpleName)
        .append(" implements ")
        .append(type.name().simpleName())
        .append(" {\n");
    renderThrowingInterfaceMethods(source, type);
    source.append("}\n");
    return new GeneratedJavaSource(type.name().packageName(), simpleName, source.toString());
  }

  private GeneratedJavaSource renderPoa(JavaMappingModel model, JavaMappedType type) {
    String simpleName = type.name().simpleName() + "POA";
    StringBuilder source = header(model, type.name().packageName());
    source
        .append("public abstract class ")
        .append(simpleName)
        .append(" implements ")
        .append(type.name().simpleName())
        .append(" {\n\n")
        .append("  public String[] _all_interfaces() {\n")
        .append("    return new String[] { \"")
        .append(escapeJava(type.name().qualifiedName()))
        .append("\" };\n")
        .append("  }\n")
        .append("}\n");
    return new GeneratedJavaSource(type.name().packageName(), simpleName, source.toString());
  }

  private StringBuilder header(JavaMappingModel model, String packageName) {
    StringBuilder source = new StringBuilder();
    source
        .append("// Generated by mundane-java-orb G6-160.\n")
        .append("// Source IDL: ")
        .append(model.sourceName())
        .append("\n")
        .append("// Mapping mode: ")
        .append(model.mode())
        .append("\n")
        .append("// Compatibility profile: compile-safe minimal IDL-to-Java slice.\n\n");
    if (!packageName.isEmpty()) {
      source.append("package ").append(packageName).append(";\n\n");
    }
    return source;
  }

  private static void renderInterface(StringBuilder source, JavaMappedType type) {
    source.append("public interface ").append(type.name().simpleName());
    if (!type.baseInterfaces().isEmpty()) {
      source.append(" extends ").append(String.join(", ", type.baseInterfaces()));
    }
    source.append(" {\n");
    for (JavaMappedAttribute attribute : type.attributes()) {
      String accessorSuffix = accessorSuffix(attribute.name());
      source
          .append("\n  ")
          .append(attribute.javaType())
          .append(" get")
          .append(accessorSuffix)
          .append("();\n");
      if (!attribute.readonly()) {
        source
            .append("\n  void set")
            .append(accessorSuffix)
            .append('(')
            .append(attribute.javaType())
            .append(' ')
            .append(attribute.name())
            .append(");\n");
      }
    }
    for (JavaMappedOperation operation : type.operations()) {
      source
          .append("\n  ")
          .append(operation.returnType())
          .append(' ')
          .append(operation.name())
          .append('(');
      source.append(
          operation.parameters().stream()
              .map(JavaSourceGenerator::parameter)
              .collect(Collectors.joining(", ")));
      source.append(')');
      if (!operation.thrownTypes().isEmpty()) {
        source.append(" throws ").append(String.join(", ", operation.thrownTypes()));
      }
      source.append(";\n");
    }
    source.append("}\n");
  }

  private static void renderStruct(StringBuilder source, JavaMappedType type) {
    source.append("public final class ").append(type.name().simpleName()).append(" {\n");
    renderFields(source, type.fields());
    renderConstructor(source, type.name().simpleName(), type.fields(), false);
    source.append("}\n");
  }

  private static void renderEnum(StringBuilder source, JavaMappedType type) {
    source.append("public enum ").append(type.name().simpleName()).append(" {\n");
    for (int index = 0; index < type.enumConstants().size(); index++) {
      String terminator = index == type.enumConstants().size() - 1 ? ";" : ",";
      source.append("  ").append(type.enumConstants().get(index)).append(terminator).append('\n');
    }
    source.append("}\n");
  }

  private static void renderException(StringBuilder source, JavaMappedType type) {
    source
        .append("public class ")
        .append(type.name().simpleName())
        .append(" extends java.lang.Exception {\n\n")
        .append("  private static final long serialVersionUID = 1L;\n");
    renderFields(source, type.fields());
    renderConstructor(source, type.name().simpleName(), type.fields(), true);
    source.append("}\n");
  }

  private static void renderTypedef(StringBuilder source, JavaMappedType type) {
    source
        .append("public final class ")
        .append(type.name().simpleName())
        .append(" {\n\n")
        .append("  public static final String VALUE_TYPE = \"")
        .append(escapeJava(type.aliasType()))
        .append("\";\n\n")
        .append("  private ")
        .append(type.name().simpleName())
        .append("() {}\n")
        .append("}\n");
  }

  private static void renderUnion(StringBuilder source, JavaMappedType type) {
    source.append("public final class ").append(type.name().simpleName()).append(" {\n");
    renderFields(source, type.fields());
    renderConstructor(source, type.name().simpleName(), type.fields(), false);
    source.append("}\n");
  }

  private static void renderNative(StringBuilder source, JavaMappedType type) {
    source
        .append("public final class ")
        .append(type.name().simpleName())
        .append(" {\n\n")
        .append("  private ")
        .append(type.name().simpleName())
        .append("() {}\n")
        .append("}\n");
  }

  private static void renderValueType(StringBuilder source, JavaMappedType type) {
    source.append("public ");
    if (type.abstractType()) {
      source.append("abstract ");
    }
    source.append("class ").append(type.name().simpleName());
    if (!type.baseInterfaces().isEmpty()) {
      source.append(" extends ").append(type.baseInterfaces().getFirst());
    }
    if (!type.supportedInterfaces().isEmpty()) {
      source.append(" implements ").append(String.join(", ", type.supportedInterfaces()));
    }
    source.append(" {\n");
    renderFields(source, type.fields());
    renderConstructor(source, type.name().simpleName(), type.fields(), false);
    for (JavaMappedAttribute attribute : type.attributes()) {
      String accessorSuffix = accessorSuffix(attribute.name());
      renderThrowingMethod(
          source, false, attribute.javaType(), "get" + accessorSuffix, List.of(), List.of());
      if (!attribute.readonly()) {
        renderThrowingMethod(
            source,
            false,
            "void",
            "set" + accessorSuffix,
            List.of(new JavaMappedParameter(attribute.javaType(), attribute.name())),
            List.of());
      }
    }
    for (JavaMappedOperation operation : type.operations()) {
      renderThrowingMethod(
          source,
          operation.factory(),
          operation.returnType(),
          operation.name(),
          operation.parameters(),
          operation.thrownTypes());
    }
    source.append("}\n");
  }

  private static void renderHolderClass(StringBuilder source, String simpleName, String valueType) {
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
  }

  private static void renderThrowingInterfaceMethods(StringBuilder source, JavaMappedType type) {
    for (JavaMappedAttribute attribute : type.attributes()) {
      String accessorSuffix = accessorSuffix(attribute.name());
      renderThrowingMethod(
          source, false, attribute.javaType(), "get" + accessorSuffix, List.of(), List.of());
      if (!attribute.readonly()) {
        renderThrowingMethod(
            source,
            false,
            "void",
            "set" + accessorSuffix,
            List.of(new JavaMappedParameter(attribute.javaType(), attribute.name())),
            List.of());
      }
    }
    for (JavaMappedOperation operation : type.operations()) {
      renderThrowingMethod(
          source,
          false,
          operation.returnType(),
          operation.name(),
          operation.parameters(),
          operation.thrownTypes());
    }
  }

  private static void renderThrowingMethod(
      StringBuilder source,
      boolean staticMethod,
      String returnType,
      String name,
      List<JavaMappedParameter> parameters,
      List<String> thrownTypes) {
    source.append("\n  public ");
    if (staticMethod) {
      source.append("static ");
    }
    source.append(returnType).append(' ').append(name).append('(');
    source.append(
        parameters.stream().map(JavaSourceGenerator::parameter).collect(Collectors.joining(", ")));
    source.append(')');
    if (!thrownTypes.isEmpty()) {
      source.append(" throws ").append(String.join(", ", thrownTypes));
    }
    source
        .append(" {\n")
        .append("    throw new UnsupportedOperationException(\"Generated compatibility stub\");\n")
        .append("  }\n");
  }

  private static void renderFields(StringBuilder source, List<JavaMappedField> fields) {
    for (JavaMappedField field : fields) {
      source
          .append("\n  public final ")
          .append(field.javaType())
          .append(' ')
          .append(field.name())
          .append(";\n");
    }
  }

  private static void renderConstructor(
      StringBuilder source, String simpleName, List<JavaMappedField> fields, boolean exception) {
    source.append("\n  public ").append(simpleName).append('(');
    source.append(
        fields.stream().map(JavaSourceGenerator::fieldParameter).collect(Collectors.joining(", ")));
    source.append(") {\n");
    if (exception) {
      source.append("    super();\n");
    }
    for (JavaMappedField field : fields) {
      source
          .append("    this.")
          .append(field.name())
          .append(" = ")
          .append(field.name())
          .append(";\n");
    }
    source.append("  }\n");
  }

  private static String parameter(JavaMappedParameter parameter) {
    return parameter.javaType() + " " + parameter.name();
  }

  private static String fieldParameter(JavaMappedField field) {
    return field.javaType() + " " + field.name();
  }

  private static String accessorSuffix(String name) {
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  private static String holderValueType(JavaMappedType type) {
    return switch (type.kind()) {
      case TYPEDEF, HOLDER -> type.aliasType();
      default -> type.name().qualifiedName();
    };
  }

  private static String castType(String javaType) {
    return switch (javaType) {
      case "boolean" -> "java.lang.Boolean";
      case "char" -> "java.lang.Character";
      case "byte" -> "java.lang.Byte";
      case "short" -> "java.lang.Short";
      case "int" -> "java.lang.Integer";
      case "long" -> "java.lang.Long";
      case "float" -> "java.lang.Float";
      case "double" -> "java.lang.Double";
      default -> javaType;
    };
  }

  private static String escapeJava(String value) {
    StringBuilder result = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      result.append(
          switch (character) {
            case '\b' -> "\\b";
            case '\t' -> "\\t";
            case '\n' -> "\\n";
            case '\f' -> "\\f";
            case '\r' -> "\\r";
            case '"' -> "\\\"";
            case '\\' -> "\\\\";
            default -> Character.toString(character);
          });
    }
    return result.toString();
  }
}
