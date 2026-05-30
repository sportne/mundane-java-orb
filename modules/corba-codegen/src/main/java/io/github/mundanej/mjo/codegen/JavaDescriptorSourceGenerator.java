package io.github.mundanej.mjo.codegen;

import io.github.mundanej.mjo.idl.ast.IdlDeclaration;
import io.github.mundanej.mjo.idl.ast.IdlDeclarator;
import io.github.mundanej.mjo.idl.ast.IdlEnum;
import io.github.mundanej.mjo.idl.ast.IdlExceptionDeclaration;
import io.github.mundanej.mjo.idl.ast.IdlField;
import io.github.mundanej.mjo.idl.ast.IdlInterface;
import io.github.mundanej.mjo.idl.ast.IdlInterfaceForward;
import io.github.mundanej.mjo.idl.ast.IdlInterfaceMember;
import io.github.mundanej.mjo.idl.ast.IdlModule;
import io.github.mundanej.mjo.idl.ast.IdlNative;
import io.github.mundanej.mjo.idl.ast.IdlOperation;
import io.github.mundanej.mjo.idl.ast.IdlParameter;
import io.github.mundanej.mjo.idl.ast.IdlParameterDirection;
import io.github.mundanej.mjo.idl.ast.IdlStruct;
import io.github.mundanej.mjo.idl.ast.IdlTypedef;
import io.github.mundanej.mjo.idl.ast.IdlUnion;
import io.github.mundanej.mjo.idl.ast.IdlUnionCase;
import io.github.mundanej.mjo.idl.ast.IdlValueBox;
import io.github.mundanej.mjo.idl.ast.IdlValueFactory;
import io.github.mundanej.mjo.idl.ast.IdlValueField;
import io.github.mundanej.mjo.idl.ast.IdlValueMember;
import io.github.mundanej.mjo.idl.ast.IdlValueType;
import io.github.mundanej.mjo.idl.java.mapping.IdlJavaMapper;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappedType;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappedTypeKind;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappingMode;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappingModel;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticModel;
import io.github.mundanej.mjo.idl.semantics.IdlSymbol;
import io.github.mundanej.mjo.idl.semantics.IdlSymbolKind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Renders deterministic static descriptor and compile-only codec Java source. */
public final class JavaDescriptorSourceGenerator {

  private static final String DEFERRED_CODEC_MESSAGE =
      "CDR codec is compile-only in G6-220; CDR string and aggregate support is deferred to G6-320.";
  private static final Set<String> PRIMITIVE_TYPES =
      Set.of(
          "boolean",
          "char",
          "wchar",
          "octet",
          "uint8",
          "short",
          "int16",
          "unsigned short",
          "uint16",
          "long",
          "int32",
          "unsigned long",
          "uint32",
          "long long",
          "int64",
          "unsigned long long",
          "uint64",
          "float",
          "double",
          "long double",
          "string",
          "wstring",
          "any",
          "Object",
          "ValueBase");

  private final IdlJavaMapper mapper = new IdlJavaMapper();

  /** Creates a stateless descriptor source generator. */
  public JavaDescriptorSourceGenerator() {}

  /** Generates descriptor and compile-only codec sources sorted by deterministic source path. */
  public List<GeneratedJavaSource> generate(IdlSemanticModel semanticModel, JavaMappingMode mode) {
    Objects.requireNonNull(semanticModel, "semanticModel");
    Objects.requireNonNull(mode, "mode");
    JavaMappingModel mappingModel = mapper.map(semanticModel, mode);
    DescriptorModel descriptorModel = DescriptorModel.from(semanticModel, mappingModel);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    for (DescriptorDeclaration declaration : descriptorModel.declarations()) {
      sources.add(renderDescriptor(mappingModel, descriptorModel, declaration));
      sources.add(renderCodec(mappingModel, declaration));
    }
    sources.add(renderRepository(mappingModel, descriptorModel));
    return sources.stream().sorted(Comparator.comparing(GeneratedJavaSource::sourcePath)).toList();
  }

  private static GeneratedJavaSource renderDescriptor(
      JavaMappingModel mappingModel,
      DescriptorModel descriptorModel,
      DescriptorDeclaration declaration) {
    String packageName = subpackage(declaration.javaType().name().packageName(), "metadata");
    String simpleName = declaration.javaType().name().simpleName() + "Descriptor";
    StringBuilder source = header(mappingModel, packageName);
    source
        .append("import io.github.mundanej.mjo.repositoryid.RepositoryId;\n")
        .append("import io.github.mundanej.mjo.typecode.IdlFieldDescriptor;\n")
        .append("import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;\n")
        .append("import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;\n")
        .append("import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;\n")
        .append("import io.github.mundanej.mjo.typecode.IdlParameterMode;\n")
        .append("import io.github.mundanej.mjo.typecode.IdlTypeKind;\n")
        .append("import io.github.mundanej.mjo.typecode.IdlTypeReference;\n")
        .append("import java.util.List;\n")
        .append("import java.util.Optional;\n\n");
    source
        .append("public final class ")
        .append(simpleName)
        .append(" {\n\n")
        .append("  public static final RepositoryId REPOSITORY_ID = RepositoryId.parse(\"")
        .append(declaration.repositoryId())
        .append("\");\n\n")
        .append("  public static final IdlTypeReference TYPE = ")
        .append(
            typeReference(
                declaration.kind(),
                declaration.idlScopedName(),
                declaration.javaName(),
                declaration.repositoryId()))
        .append(";\n");
    renderOperationConstants(source, descriptorModel, declaration);
    source
        .append("\n  public static final IdlGeneratedTypeDescriptor DESCRIPTOR =\n")
        .append("      new IdlGeneratedTypeDescriptor(\n")
        .append("          ")
        .append(typeKind(declaration.kind()))
        .append(",\n")
        .append("          \"")
        .append(escapeJava(declaration.idlScopedName()))
        .append("\",\n")
        .append("          \"")
        .append(escapeJava(declaration.javaName()))
        .append("\",\n")
        .append("          REPOSITORY_ID,\n")
        .append("          ")
        .append(fields(descriptorModel, declaration))
        .append(",\n")
        .append("          ")
        .append(enumConstants(declaration))
        .append(",\n")
        .append("          ")
        .append(operations(declaration))
        .append(");\n\n")
        .append("  private ")
        .append(simpleName)
        .append("() {}\n")
        .append("}\n");
    return new GeneratedJavaSource(packageName, simpleName, source.toString());
  }

  private static void renderOperationConstants(
      StringBuilder source, DescriptorModel descriptorModel, DescriptorDeclaration declaration) {
    for (OperationDescriptor operation : declaration.operations()) {
      source
          .append("\n  public static final IdlOperationDescriptor ")
          .append(constantName(operation.name()))
          .append(" =\n")
          .append("      new IdlOperationDescriptor(\n")
          .append("          \"")
          .append(escapeJava(operation.name()))
          .append("\",\n")
          .append("          ")
          .append(typeReference(descriptorModel, operation.returnType(), declaration.modules()))
          .append(",\n")
          .append("          ")
          .append(parameters(descriptorModel, operation.parameters(), declaration.modules()))
          .append(",\n")
          .append("          ")
          .append(raises(descriptorModel, operation.raises(), declaration.modules()))
          .append(");\n");
    }
  }

  private static GeneratedJavaSource renderCodec(
      JavaMappingModel mappingModel, DescriptorDeclaration declaration) {
    String packageName = subpackage(declaration.javaType().name().packageName(), "codec");
    String simpleName = declaration.javaType().name().simpleName() + "Codec";
    StringBuilder source = header(mappingModel, packageName);
    source
        .append("import io.github.mundanej.mjo.typecode.IdlCodec;\n")
        .append("import io.github.mundanej.mjo.typecode.UnsupportedIdlCodec;\n\n")
        .append("public final class ")
        .append(simpleName)
        .append(" {\n\n")
        .append("  private static final String DEFERRED_CODEC_MESSAGE = \"")
        .append(escapeJava(DEFERRED_CODEC_MESSAGE))
        .append("\";\n");
    if (declaration.kind() == IdlSymbolKind.INTERFACE) {
      for (OperationDescriptor operation : declaration.operations()) {
        String name = constantName(operation.name());
        source
            .append("\n  public static final IdlCodec<java.lang.Object> ")
            .append(name)
            .append("_REQUEST = new UnsupportedIdlCodec<>(DEFERRED_CODEC_MESSAGE);\n")
            .append("\n  public static final IdlCodec<java.lang.Object> ")
            .append(name)
            .append("_REPLY = new UnsupportedIdlCodec<>(DEFERRED_CODEC_MESSAGE);\n");
      }
    } else {
      source
          .append("\n  public static final IdlCodec<")
          .append(declaration.javaName())
          .append("> VALUE = new UnsupportedIdlCodec<>(DEFERRED_CODEC_MESSAGE);\n");
    }
    source.append("\n  private ").append(simpleName).append("() {}\n").append("}\n");
    return new GeneratedJavaSource(packageName, simpleName, source.toString());
  }

  private static GeneratedJavaSource renderRepository(
      JavaMappingModel mappingModel, DescriptorModel descriptorModel) {
    String packageName = repositoryPackage(descriptorModel);
    StringBuilder source = header(mappingModel, packageName);
    source
        .append("import io.github.mundanej.mjo.ir.StaticInterfaceRepository;\n")
        .append("import java.util.List;\n\n")
        .append("public final class GeneratedInterfaceRepository {\n\n")
        .append("  public static final StaticInterfaceRepository REPOSITORY =\n")
        .append("      StaticInterfaceRepository.of(\n")
        .append("          ")
        .append(repositoryDescriptors(descriptorModel))
        .append(");\n\n")
        .append("  private GeneratedInterfaceRepository() {}\n")
        .append("}\n");
    return new GeneratedJavaSource(packageName, "GeneratedInterfaceRepository", source.toString());
  }

  private static String fields(DescriptorModel descriptorModel, DescriptorDeclaration declaration) {
    if (declaration.fields().isEmpty()) {
      return "List.of()";
    }
    return declaration.fields().stream()
        .map(
            field ->
                "new IdlFieldDescriptor(\""
                    + escapeJava(field.name())
                    + "\", "
                    + typeReference(descriptorModel, field.typeName(), declaration.modules())
                    + ")")
        .collect(java.util.stream.Collectors.joining(",\n              ", "List.of(", ")"));
  }

  private static String enumConstants(DescriptorDeclaration declaration) {
    if (declaration.enumConstants().isEmpty()) {
      return "List.of()";
    }
    return declaration.enumConstants().stream()
        .map(value -> "\"" + escapeJava(value) + "\"")
        .collect(java.util.stream.Collectors.joining(", ", "List.of(", ")"));
  }

  private static String operations(DescriptorDeclaration declaration) {
    if (declaration.operations().isEmpty()) {
      return "List.of()";
    }
    return declaration.operations().stream()
        .map(operation -> constantName(operation.name()))
        .collect(java.util.stream.Collectors.joining(", ", "List.of(", ")"));
  }

  private static String parameters(
      DescriptorModel descriptorModel, List<ParameterDescriptor> parameters, List<String> modules) {
    if (parameters.isEmpty()) {
      return "List.of()";
    }
    return parameters.stream()
        .map(
            parameter ->
                "new IdlParameterDescriptor(\""
                    + escapeJava(parameter.name())
                    + "\", IdlParameterMode."
                    + parameter.mode()
                    + ", "
                    + typeReference(descriptorModel, parameter.typeName(), modules)
                    + ")")
        .collect(java.util.stream.Collectors.joining(",\n              ", "List.of(", ")"));
  }

  private static String raises(
      DescriptorModel descriptorModel, List<String> raises, List<String> modules) {
    if (raises.isEmpty()) {
      return "List.of()";
    }
    return raises.stream()
        .map(raised -> typeReference(descriptorModel, raised, modules))
        .collect(java.util.stream.Collectors.joining(",\n              ", "List.of(", ")"));
  }

  private static String typeReference(
      DescriptorModel descriptorModel, String idlType, List<String> modules) {
    if ("void".equals(idlType)) {
      return "new IdlTypeReference(IdlTypeKind.VOID, \"void\", \"void\", Optional.empty())";
    }
    if (PRIMITIVE_TYPES.contains(idlType)) {
      return "new IdlTypeReference(IdlTypeKind.PRIMITIVE, \""
          + escapeJava(idlType)
          + "\", \""
          + escapeJava(javaPrimitiveName(idlType))
          + "\", Optional.empty())";
    }
    if (idlType.startsWith("string<") || idlType.startsWith("wstring<")) {
      return "new IdlTypeReference(IdlTypeKind.PRIMITIVE, \""
          + escapeJava(idlType)
          + "\", \"java.lang.String\", Optional.empty())";
    }
    if (idlType.startsWith("sequence<")) {
      return "new IdlTypeReference(IdlTypeKind.PRIMITIVE, \""
          + escapeJava(idlType)
          + "\", \"java.lang.Object[]\", Optional.empty())";
    }
    return descriptorModel
        .resolve(idlType, modules)
        .map(
            declaration ->
                typeReference(
                    declaration.kind(),
                    declaration.idlScopedName(),
                    declaration.javaName(),
                    declaration.repositoryId()))
        .orElseGet(() -> descriptorModel.fallbackTypeReference(idlType, modules));
  }

  private static String typeReference(
      IdlSymbolKind kind, String idlScopedName, String javaName, String repositoryId) {
    return "new IdlTypeReference("
        + typeKind(kind)
        + ", \""
        + escapeJava(idlScopedName)
        + "\", \""
        + escapeJava(javaName)
        + "\", Optional.of(RepositoryId.parse(\""
        + repositoryId
        + "\")))";
  }

  private static String typeKind(IdlSymbolKind kind) {
    return switch (kind) {
      case INTERFACE -> "IdlTypeKind.INTERFACE";
      case STRUCT -> "IdlTypeKind.STRUCT";
      case ENUM -> "IdlTypeKind.ENUM";
      case EXCEPTION -> "IdlTypeKind.EXCEPTION";
      case TYPEDEF -> "IdlTypeKind.TYPEDEF";
      case UNION -> "IdlTypeKind.UNION";
      case NATIVE -> "IdlTypeKind.NATIVE";
      case VALUE_BOX -> "IdlTypeKind.VALUE_BOX";
      case VALUETYPE -> "IdlTypeKind.VALUETYPE";
      default -> throw new IllegalArgumentException("Unsupported descriptor kind: " + kind);
    };
  }

  private static String javaPrimitiveName(String idlType) {
    return switch (idlType) {
      case "boolean" -> "boolean";
      case "char", "wchar" -> "char";
      case "octet", "uint8" -> "short";
      case "short", "int16" -> "short";
      case "unsigned short", "uint16" -> "int";
      case "long", "int32" -> "int";
      case "unsigned long", "uint32" -> "long";
      case "long long", "int64" -> "long";
      case "unsigned long long", "uint64" -> "java.math.BigInteger";
      case "float" -> "float";
      case "double" -> "double";
      case "long double" -> "java.math.BigDecimal";
      case "string", "wstring" -> "java.lang.String";
      case "any", "Object", "ValueBase" -> "java.lang.Object";
      default -> throw new IllegalArgumentException("Unsupported primitive type: " + idlType);
    };
  }

  private static StringBuilder header(JavaMappingModel mappingModel, String packageName) {
    StringBuilder source = new StringBuilder();
    source
        .append("// Generated by mundane-java-orb G6-220.\n")
        .append("// Source IDL: ")
        .append(mappingModel.sourceName())
        .append("\n")
        .append("// Mapping mode: ")
        .append(mappingModel.mode())
        .append("\n")
        .append("// Compatibility profile: static descriptor and compile-only codec slice.\n\n");
    if (!packageName.isEmpty()) {
      source.append("package ").append(packageName).append(";\n\n");
    }
    return source;
  }

  private static String subpackage(String packageName, String child) {
    return packageName.isEmpty() ? child : packageName + "." + child;
  }

  private static String repositoryPackage(DescriptorModel descriptorModel) {
    List<String> packages =
        descriptorModel.declarations().stream()
            .map(JavaDescriptorSourceGenerator::descriptorPackage)
            .distinct()
            .toList();
    if (packages.size() == 1) {
      return packages.get(0);
    }
    return "metadata";
  }

  private static String descriptorPackage(DescriptorDeclaration declaration) {
    return subpackage(declaration.javaType().name().packageName(), "metadata");
  }

  private static String repositoryDescriptors(DescriptorModel descriptorModel) {
    if (descriptorModel.declarations().isEmpty()) {
      return "List.of()";
    }
    return descriptorModel.declarations().stream()
        .map(declaration -> descriptorQualifiedName(declaration) + ".DESCRIPTOR")
        .collect(java.util.stream.Collectors.joining(",\n              ", "List.of(", ")"));
  }

  private static String descriptorQualifiedName(DescriptorDeclaration declaration) {
    return descriptorPackage(declaration)
        + "."
        + declaration.javaType().name().simpleName()
        + "Descriptor";
  }

  private static String repositoryId(String idlScopedName) {
    String normalized = idlScopedName.startsWith("::") ? idlScopedName.substring(2) : idlScopedName;
    return "IDL:" + normalized.replace("::", "/") + ":1.0";
  }

  private static String absoluteName(List<String> modules, String name) {
    String normalized = name.startsWith("::") ? name.substring(2) : name;
    if (modules.isEmpty()) {
      return "::" + normalized;
    }
    return "::" + String.join("::", modules) + "::" + normalized;
  }

  private static String constantName(String idlName) {
    StringBuilder result = new StringBuilder();
    for (int index = 0; index < idlName.length(); index++) {
      char character = idlName.charAt(index);
      if (Character.isLetterOrDigit(character)) {
        if (index > 0
            && Character.isUpperCase(character)
            && Character.isLowerCase(idlName.charAt(index - 1))) {
          result.append('_');
        }
        result.append(Character.toUpperCase(character));
      } else {
        result.append('_');
      }
    }
    return result.toString();
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

  private record FieldDescriptor(String name, String typeName) {}

  private record ParameterDescriptor(String name, IdlParameterDirection mode, String typeName) {}

  private record OperationDescriptor(
      String name, String returnType, List<ParameterDescriptor> parameters, List<String> raises) {}

  private record DescriptorDeclaration(
      IdlSymbolKind kind,
      String idlScopedName,
      String javaName,
      String repositoryId,
      List<String> modules,
      JavaMappedType javaType,
      List<FieldDescriptor> fields,
      List<String> enumConstants,
      List<OperationDescriptor> operations) {}

  private record DescriptorModel(
      IdlSemanticModel semanticModel,
      List<DescriptorDeclaration> declarations,
      Map<String, DescriptorDeclaration> declarationsByIdlName) {

    private static DescriptorModel from(
        IdlSemanticModel semanticModel, JavaMappingModel mappingModel) {
      List<DescriptorDeclaration> declarations = new ArrayList<>();
      collect(
          semanticModel.translationUnit().declarations(),
          List.of(),
          semanticModel,
          mappingModel.types(),
          new int[] {0},
          declarations);
      Map<String, DescriptorDeclaration> byName = new HashMap<>();
      for (DescriptorDeclaration declaration : declarations) {
        byName.put(declaration.idlScopedName(), declaration);
      }
      return new DescriptorModel(semanticModel, List.copyOf(declarations), Map.copyOf(byName));
    }

    private java.util.Optional<DescriptorDeclaration> resolve(
        String idlName, List<String> modules) {
      IdlSymbol symbol = resolveSymbol(idlName, modules);
      return java.util.Optional.ofNullable(declarationsByIdlName.get(symbol.qualifiedName()));
    }

    private String fallbackTypeReference(String idlName, List<String> modules) {
      IdlSymbol symbol = resolveSymbol(idlName, modules);
      return "new IdlTypeReference(IdlTypeKind.PRIMITIVE, \""
          + escapeJava(symbol.qualifiedName())
          + "\", \"java.lang.Object\", Optional.of(RepositoryId.parse(\""
          + symbol
              .repositoryId()
              .orElseGet(() -> JavaDescriptorSourceGenerator.repositoryId(symbol.qualifiedName()))
          + "\")))";
    }

    private IdlSymbol resolveSymbol(String idlName, List<String> modules) {
      IdlSymbol symbol;
      if (idlName.startsWith("::")) {
        symbol = symbol(idlName);
      } else {
        symbol = null;
        for (int count = modules.size(); count >= 0; count--) {
          List<String> prefix = modules.subList(0, count);
          java.util.Optional<IdlSymbol> resolved =
              semanticModel.findSymbol(absoluteName(prefix, idlName));
          if (resolved.isPresent()) {
            symbol = resolved.orElseThrow();
            break;
          }
        }
        if (symbol == null) {
          throw new IllegalArgumentException("Unresolved IDL name in descriptor model: " + idlName);
        }
      }
      return symbol;
    }

    private IdlSymbol symbol(String qualifiedName) {
      return semanticModel
          .findSymbol(qualifiedName)
          .orElseThrow(
              () -> new IllegalArgumentException("Missing semantic symbol: " + qualifiedName));
    }

    private static void collect(
        List<IdlDeclaration> declarations,
        List<String> modules,
        IdlSemanticModel semanticModel,
        List<JavaMappedType> javaTypes,
        int[] index,
        List<DescriptorDeclaration> output) {
      for (IdlDeclaration declaration : declarations) {
        if (declaration instanceof IdlModule module) {
          List<String> childModules = new ArrayList<>(modules);
          childModules.add(module.name());
          collect(module.declarations(), childModules, semanticModel, javaTypes, index, output);
        } else if (declaration instanceof IdlStruct struct) {
          output.add(structDescriptor(struct, modules, semanticModel, javaTypes.get(index[0]++)));
        } else if (declaration instanceof IdlEnum idlEnum) {
          output.add(enumDescriptor(idlEnum, modules, semanticModel, javaTypes.get(index[0]++)));
        } else if (declaration instanceof IdlExceptionDeclaration exception) {
          output.add(
              exceptionDescriptor(exception, modules, semanticModel, javaTypes.get(index[0]++)));
        } else if (declaration instanceof IdlInterface idlInterface) {
          output.add(
              interfaceDescriptor(idlInterface, modules, semanticModel, javaTypes.get(index[0]++)));
        } else if (declaration instanceof IdlTypedef typedef) {
          for (int typedefIndex = 0; typedefIndex < typedef.declarators().size(); typedefIndex++) {
            output.add(
                typedefDescriptor(
                    typedef,
                    typedef.declarators().get(typedefIndex),
                    modules,
                    semanticModel,
                    javaTypes.get(index[0]++)));
          }
        } else if (declaration instanceof IdlUnion union) {
          output.add(unionDescriptor(union, modules, semanticModel, javaTypes.get(index[0]++)));
        } else if (declaration instanceof IdlNative nativeDeclaration) {
          output.add(
              nativeDescriptor(
                  nativeDeclaration, modules, semanticModel, javaTypes.get(index[0]++)));
        } else if (declaration instanceof IdlValueBox valueBox) {
          output.add(
              valueBoxDescriptor(valueBox, modules, semanticModel, javaTypes.get(index[0]++)));
        } else if (declaration instanceof IdlValueType valueType) {
          output.add(
              valueTypeDescriptor(valueType, modules, semanticModel, javaTypes.get(index[0]++)));
        } else if (declaration instanceof IdlInterfaceForward
            && index[0] < javaTypes.size()
            && javaTypes.get(index[0]).kind() == JavaMappedTypeKind.INTERFACE_FORWARD) {
          index[0]++;
        }
      }
    }

    private static DescriptorDeclaration structDescriptor(
        IdlStruct struct,
        List<String> modules,
        IdlSemanticModel semanticModel,
        JavaMappedType javaType) {
      String idlName = absoluteName(modules, struct.name());
      return new DescriptorDeclaration(
          IdlSymbolKind.STRUCT,
          idlName,
          javaType.name().qualifiedName(),
          repositoryId(semanticModel, idlName),
          List.copyOf(modules),
          javaType,
          fields(struct.fields()),
          List.of(),
          List.of());
    }

    private static DescriptorDeclaration enumDescriptor(
        IdlEnum idlEnum,
        List<String> modules,
        IdlSemanticModel semanticModel,
        JavaMappedType javaType) {
      String idlName = absoluteName(modules, idlEnum.name());
      return new DescriptorDeclaration(
          IdlSymbolKind.ENUM,
          idlName,
          javaType.name().qualifiedName(),
          repositoryId(semanticModel, idlName),
          List.copyOf(modules),
          javaType,
          List.of(),
          idlEnum.enumerators(),
          List.of());
    }

    private static DescriptorDeclaration exceptionDescriptor(
        IdlExceptionDeclaration exception,
        List<String> modules,
        IdlSemanticModel semanticModel,
        JavaMappedType javaType) {
      String idlName = absoluteName(modules, exception.name());
      return new DescriptorDeclaration(
          IdlSymbolKind.EXCEPTION,
          idlName,
          javaType.name().qualifiedName(),
          repositoryId(semanticModel, idlName),
          List.copyOf(modules),
          javaType,
          fields(exception.fields()),
          List.of(),
          List.of());
    }

    private static DescriptorDeclaration interfaceDescriptor(
        IdlInterface idlInterface,
        List<String> modules,
        IdlSemanticModel semanticModel,
        JavaMappedType javaType) {
      List<OperationDescriptor> operations = new ArrayList<>();
      for (IdlInterfaceMember member : idlInterface.members()) {
        if (member instanceof IdlOperation operation) {
          operations.add(operation(operation));
        }
      }
      String idlName = absoluteName(modules, idlInterface.name());
      return new DescriptorDeclaration(
          IdlSymbolKind.INTERFACE,
          idlName,
          javaType.name().qualifiedName(),
          repositoryId(semanticModel, idlName),
          List.copyOf(modules),
          javaType,
          List.of(),
          List.of(),
          operations);
    }

    private static DescriptorDeclaration typedefDescriptor(
        IdlTypedef typedef,
        IdlDeclarator declarator,
        List<String> modules,
        IdlSemanticModel semanticModel,
        JavaMappedType javaType) {
      String idlName = absoluteName(modules, declarator.name());
      return new DescriptorDeclaration(
          IdlSymbolKind.TYPEDEF,
          idlName,
          javaType.name().qualifiedName(),
          repositoryId(semanticModel, idlName),
          List.copyOf(modules),
          javaType,
          List.of(new FieldDescriptor("value", typedef.type().name())),
          List.of(),
          List.of());
    }

    private static DescriptorDeclaration unionDescriptor(
        IdlUnion union,
        List<String> modules,
        IdlSemanticModel semanticModel,
        JavaMappedType javaType) {
      String idlName = absoluteName(modules, union.name());
      return new DescriptorDeclaration(
          IdlSymbolKind.UNION,
          idlName,
          javaType.name().qualifiedName(),
          repositoryId(semanticModel, idlName),
          List.copyOf(modules),
          javaType,
          unionFields(union.cases()),
          List.of(),
          List.of());
    }

    private static DescriptorDeclaration nativeDescriptor(
        IdlNative nativeDeclaration,
        List<String> modules,
        IdlSemanticModel semanticModel,
        JavaMappedType javaType) {
      String idlName = absoluteName(modules, nativeDeclaration.name());
      return new DescriptorDeclaration(
          IdlSymbolKind.NATIVE,
          idlName,
          javaType.name().qualifiedName(),
          repositoryId(semanticModel, idlName),
          List.copyOf(modules),
          javaType,
          List.of(),
          List.of(),
          List.of());
    }

    private static DescriptorDeclaration valueBoxDescriptor(
        IdlValueBox valueBox,
        List<String> modules,
        IdlSemanticModel semanticModel,
        JavaMappedType javaType) {
      String idlName = absoluteName(modules, valueBox.name());
      return new DescriptorDeclaration(
          IdlSymbolKind.VALUE_BOX,
          idlName,
          javaType.name().qualifiedName(),
          repositoryId(semanticModel, idlName),
          List.copyOf(modules),
          javaType,
          List.of(new FieldDescriptor("value", valueBox.boxedType().name())),
          List.of(),
          List.of());
    }

    private static DescriptorDeclaration valueTypeDescriptor(
        IdlValueType valueType,
        List<String> modules,
        IdlSemanticModel semanticModel,
        JavaMappedType javaType) {
      String idlName = absoluteName(modules, valueType.name());
      return new DescriptorDeclaration(
          IdlSymbolKind.VALUETYPE,
          idlName,
          javaType.name().qualifiedName(),
          repositoryId(semanticModel, idlName),
          List.copyOf(modules),
          javaType,
          valueFields(valueType.members()),
          List.of(),
          valueOperations(valueType, modules));
    }

    private static List<FieldDescriptor> fields(List<IdlField> fields) {
      return fields.stream()
          .map(field -> new FieldDescriptor(field.name(), field.type().name()))
          .toList();
    }

    private static List<FieldDescriptor> unionFields(List<IdlUnionCase> cases) {
      return cases.stream()
          .map(
              unionCase ->
                  new FieldDescriptor(unionCase.declarator().name(), unionCase.type().name()))
          .toList();
    }

    private static List<FieldDescriptor> valueFields(List<IdlValueMember> members) {
      List<FieldDescriptor> fields = new ArrayList<>();
      for (IdlValueMember member : members) {
        if (member instanceof IdlValueField valueField) {
          for (IdlDeclarator declarator : valueField.declarators()) {
            fields.add(new FieldDescriptor(declarator.name(), valueField.type().name()));
          }
        }
      }
      return fields;
    }

    private static List<OperationDescriptor> valueOperations(
        IdlValueType valueType, List<String> modules) {
      List<OperationDescriptor> operations = new ArrayList<>();
      for (IdlValueMember member : valueType.members()) {
        if (member instanceof IdlOperation operation) {
          operations.add(operation(operation));
        } else if (member instanceof IdlValueFactory factory) {
          operations.add(valueFactory(valueType, modules, factory));
        }
      }
      return operations;
    }

    private static OperationDescriptor operation(IdlOperation operation) {
      return new OperationDescriptor(
          operation.name(),
          operation.returnType().name(),
          operation.parameters().stream().map(DescriptorModel::parameter).toList(),
          operation.raises());
    }

    private static OperationDescriptor valueFactory(
        IdlValueType valueType, List<String> modules, IdlValueFactory factory) {
      return new OperationDescriptor(
          factory.name(),
          absoluteName(modules, valueType.name()),
          factory.parameters().stream().map(DescriptorModel::parameter).toList(),
          factory.raises());
    }

    private static ParameterDescriptor parameter(IdlParameter parameter) {
      return new ParameterDescriptor(
          parameter.name(), parameter.direction(), parameter.type().name());
    }

    private static String repositoryId(IdlSemanticModel semanticModel, String idlName) {
      return semanticModel
          .findSymbol(idlName)
          .flatMap(IdlSymbol::repositoryId)
          .orElseGet(() -> JavaDescriptorSourceGenerator.repositoryId(idlName));
    }
  }
}
