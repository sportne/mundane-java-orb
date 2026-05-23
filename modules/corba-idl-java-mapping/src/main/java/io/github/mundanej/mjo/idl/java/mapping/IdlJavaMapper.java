package io.github.mundanej.mjo.idl.java.mapping;

import io.github.mundanej.mjo.idl.ast.IdlAttribute;
import io.github.mundanej.mjo.idl.ast.IdlConstant;
import io.github.mundanej.mjo.idl.ast.IdlDeclaration;
import io.github.mundanej.mjo.idl.ast.IdlDeclarator;
import io.github.mundanej.mjo.idl.ast.IdlEnum;
import io.github.mundanej.mjo.idl.ast.IdlExceptionDeclaration;
import io.github.mundanej.mjo.idl.ast.IdlField;
import io.github.mundanej.mjo.idl.ast.IdlInterface;
import io.github.mundanej.mjo.idl.ast.IdlInterfaceForward;
import io.github.mundanej.mjo.idl.ast.IdlInterfaceMember;
import io.github.mundanej.mjo.idl.ast.IdlModule;
import io.github.mundanej.mjo.idl.ast.IdlOperation;
import io.github.mundanej.mjo.idl.ast.IdlParameter;
import io.github.mundanej.mjo.idl.ast.IdlParameterDirection;
import io.github.mundanej.mjo.idl.ast.IdlStruct;
import io.github.mundanej.mjo.idl.ast.IdlTypeReference;
import io.github.mundanej.mjo.idl.ast.IdlTypedef;
import io.github.mundanej.mjo.idl.ast.IdlUnion;
import io.github.mundanej.mjo.idl.ast.IdlUnionCase;
import io.github.mundanej.mjo.idl.semantics.IdlConstantValue;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticModel;
import io.github.mundanej.mjo.idl.semantics.IdlSymbol;
import io.github.mundanej.mjo.idl.semantics.IdlSymbolKind;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Maps a valid IDL semantic model to a deterministic compile-safe Java source model. */
public final class IdlJavaMapper {

  private static final String CONSTANT_HOLDER = "IdlConstants";
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
          "record",
          "return",
          "sealed",
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
          "var",
          "void",
          "volatile",
          "while",
          "yield");

  /** Creates a stateless mapper. */
  public IdlJavaMapper() {}

  /** Maps one valid semantic model using the requested Java mapping mode. */
  public JavaMappingModel map(IdlSemanticModel semanticModel, JavaMappingMode mode) {
    Objects.requireNonNull(semanticModel, "semanticModel");
    Objects.requireNonNull(mode, "mode");
    Mapper mapper = new Mapper(semanticModel, mode);
    return mapper.map();
  }

  private static final class Mapper {

    private final IdlSemanticModel semanticModel;
    private final JavaMappingMode mode;
    private final List<JavaMappedType> types = new ArrayList<>();
    private final List<JavaMappedType> syntheticHolders = new ArrayList<>();
    private final List<JavaMappedConstantScope> constantScopes = new ArrayList<>();
    private final Set<String> fullInterfaceNames = new LinkedHashSet<>();
    private final Set<String> syntheticHolderNames = new LinkedHashSet<>();

    private Mapper(IdlSemanticModel semanticModel, JavaMappingMode mode) {
      this.semanticModel = semanticModel;
      this.mode = mode;
      collectFullInterfaceNames(semanticModel.translationUnit().declarations(), List.of());
    }

    private JavaMappingModel map() {
      mapDeclarations(semanticModel.translationUnit().declarations(), List.of());
      List<JavaMappedType> allTypes = new ArrayList<>(types);
      allTypes.addAll(syntheticHolders);
      return new JavaMappingModel(
          mode,
          semanticModel.translationUnit().span().start().sourceName(),
          allTypes,
          constantScopes);
    }

    private void collectFullInterfaceNames(
        List<IdlDeclaration> declarations, List<String> modules) {
      for (IdlDeclaration declaration : declarations) {
        if (declaration instanceof IdlModule module) {
          List<String> childModules = new ArrayList<>(modules);
          childModules.add(module.name());
          collectFullInterfaceNames(module.declarations(), childModules);
        } else if (declaration instanceof IdlInterface idlInterface) {
          fullInterfaceNames.add(absoluteName(modules, idlInterface.name()));
        }
      }
    }

    private void mapDeclarations(List<IdlDeclaration> declarations, List<String> modules) {
      List<JavaMappedConstant> constants = new ArrayList<>();
      for (IdlDeclaration declaration : declarations) {
        if (declaration instanceof IdlConstant constant) {
          constants.add(mapConstant(constant, modules));
        } else {
          mapDeclaration(declaration, modules);
        }
      }
      flushConstants(constants, modules, declarations);
    }

    private void mapDeclaration(IdlDeclaration declaration, List<String> modules) {
      if (declaration instanceof IdlModule module) {
        List<String> childModules = new ArrayList<>(modules);
        childModules.add(module.name());
        mapDeclarations(module.declarations(), childModules);
      } else if (declaration instanceof IdlStruct struct) {
        types.add(mapStruct(struct, modules));
      } else if (declaration instanceof IdlEnum idlEnum) {
        types.add(mapEnum(idlEnum, modules));
      } else if (declaration instanceof IdlExceptionDeclaration exception) {
        types.add(mapException(exception, modules));
      } else if (declaration instanceof IdlTypedef typedef) {
        types.addAll(mapTypedef(typedef, modules));
      } else if (declaration instanceof IdlUnion union) {
        types.add(mapUnion(union, modules));
      } else if (declaration instanceof IdlInterface idlInterface) {
        types.add(mapInterface(idlInterface, modules));
      } else if (declaration instanceof IdlInterfaceForward forward) {
        if (!fullInterfaceNames.contains(absoluteName(modules, forward.name()))) {
          types.add(mapInterfaceForward(forward, modules));
        }
      }
    }

    private JavaMappedType mapStruct(IdlStruct struct, List<String> modules) {
      List<JavaMappedField> fields =
          struct.fields().stream().map(field -> mapField(field, modules)).toList();
      return new JavaMappedType(
          JavaMappedTypeKind.STRUCT,
          mappedTypeName(struct.name(), modules),
          fields,
          List.of(),
          List.of(),
          List.of());
    }

    private List<JavaMappedType> mapTypedef(IdlTypedef typedef, List<String> modules) {
      List<JavaMappedType> mapped = new ArrayList<>();
      for (IdlDeclarator declarator : typedef.declarators()) {
        mapped.add(
            new JavaMappedType(
                JavaMappedTypeKind.TYPEDEF,
                mappedTypeName(declarator.name(), modules),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                mapType(typedef.type(), declarator, modules)));
      }
      return mapped;
    }

    private JavaMappedType mapUnion(IdlUnion union, List<String> modules) {
      List<JavaMappedField> fields =
          union.cases().stream().map(unionCase -> mapUnionCase(unionCase, modules)).toList();
      return new JavaMappedType(
          JavaMappedTypeKind.UNION,
          mappedTypeName(union.name(), modules),
          fields,
          List.of(),
          List.of(),
          List.of());
    }

    private JavaMappedType mapEnum(IdlEnum idlEnum, List<String> modules) {
      return new JavaMappedType(
          JavaMappedTypeKind.ENUM,
          mappedTypeName(idlEnum.name(), modules),
          List.of(),
          idlEnum.enumerators().stream().map(IdlJavaMapper::constantName).toList(),
          List.of(),
          List.of());
    }

    private JavaMappedType mapException(IdlExceptionDeclaration exception, List<String> modules) {
      List<JavaMappedField> fields =
          exception.fields().stream().map(field -> mapField(field, modules)).toList();
      return new JavaMappedType(
          JavaMappedTypeKind.EXCEPTION,
          mappedTypeName(exception.name(), modules),
          fields,
          List.of(),
          List.of(),
          List.of());
    }

    private JavaMappedType mapInterface(IdlInterface idlInterface, List<String> modules) {
      List<JavaMappedOperation> operations = new ArrayList<>();
      List<JavaMappedAttribute> attributes = new ArrayList<>();
      for (IdlInterfaceMember member : idlInterface.members()) {
        if (member instanceof IdlOperation operation) {
          operations.add(mapOperation(operation, modules));
        } else if (member instanceof IdlAttribute attribute) {
          attributes.addAll(mapAttribute(attribute, modules));
        }
      }
      return new JavaMappedType(
          JavaMappedTypeKind.INTERFACE,
          mappedTypeName(idlInterface.name(), modules),
          List.of(),
          List.of(),
          operations,
          attributes,
          idlInterface.baseInterfaces().stream().map(base -> mapType(base, modules)).toList(),
          "");
    }

    private JavaMappedType mapInterfaceForward(IdlInterfaceForward forward, List<String> modules) {
      return new JavaMappedType(
          JavaMappedTypeKind.INTERFACE_FORWARD,
          mappedTypeName(forward.name(), modules),
          List.of(),
          List.of(),
          List.of(),
          List.of());
    }

    private JavaMappedField mapField(IdlField field, List<String> modules) {
      return new JavaMappedField(
          mapType(field.type(), field.declarator(), modules), memberName(field.name()));
    }

    private JavaMappedField mapUnionCase(IdlUnionCase unionCase, List<String> modules) {
      return new JavaMappedField(
          mapType(unionCase.type(), unionCase.declarator(), modules),
          memberName(unionCase.declarator().name()));
    }

    private JavaMappedOperation mapOperation(IdlOperation operation, List<String> modules) {
      List<JavaMappedParameter> parameters =
          operation.parameters().stream()
              .map(parameter -> mapParameter(parameter, modules))
              .toList();
      List<String> thrownTypes =
          operation.raises().stream().map(raised -> mapType(raised, modules)).toList();
      return new JavaMappedOperation(
          mapType(operation.returnType().name(), modules),
          memberName(operation.name()),
          parameters,
          thrownTypes);
    }

    private JavaMappedParameter mapParameter(IdlParameter parameter, List<String> modules) {
      String javaType =
          parameter.direction() == IdlParameterDirection.IN
              ? mapType(parameter.type(), modules)
              : holderType(parameter.type(), modules);
      return new JavaMappedParameter(javaType, memberName(parameter.name()), parameter.direction());
    }

    private List<JavaMappedAttribute> mapAttribute(IdlAttribute attribute, List<String> modules) {
      return attribute.declarators().stream()
          .map(
              declarator ->
                  new JavaMappedAttribute(
                      mapType(attribute.type(), declarator, modules),
                      memberName(declarator.name()),
                      attribute.readonly()))
          .toList();
    }

    private JavaMappedConstant mapConstant(IdlConstant constant, List<String> modules) {
      IdlSymbol symbol = symbol(absoluteName(modules, constant.name()));
      IdlConstantValue value = symbol.constantValue().orElseThrow();
      String javaType = mapType(symbol.resolvedTypeName().orElse(constant.type().name()), modules);
      return new JavaMappedConstant(
          javaType, constantName(constant.name()), initializer(mode, javaType, value));
    }

    private void flushConstants(
        List<JavaMappedConstant> constants,
        List<String> modules,
        List<IdlDeclaration> declarations) {
      if (!constants.isEmpty()) {
        constantScopes.add(
            new JavaMappedConstantScope(
                new JavaMappedName(packageName(modules), constantHolderName(declarations)),
                constants));
        constants.clear();
      }
    }

    private static String constantHolderName(List<IdlDeclaration> declarations) {
      List<String> generatedTypeNames =
          declarations.stream()
              .map(Mapper::generatedTypeSimpleName)
              .flatMap(Optional::stream)
              .toList();
      String candidate = CONSTANT_HOLDER;
      while (generatedTypeNames.contains(candidate)) {
        candidate += "_";
      }
      return candidate;
    }

    private static Optional<String> generatedTypeSimpleName(IdlDeclaration declaration) {
      if (declaration instanceof IdlStruct struct) {
        return Optional.of(typeName(struct.name()));
      }
      if (declaration instanceof IdlEnum idlEnum) {
        return Optional.of(typeName(idlEnum.name()));
      }
      if (declaration instanceof IdlExceptionDeclaration exception) {
        return Optional.of(typeName(exception.name()));
      }
      if (declaration instanceof IdlInterface idlInterface) {
        return Optional.of(typeName(idlInterface.name()));
      }
      return Optional.empty();
    }

    private String mapType(IdlTypeReference type, List<String> modules) {
      return switch (type.kind()) {
        case NAMED -> mapType(type.name(), modules);
        case BOUNDED_STRING -> "java.lang.String";
        case SEQUENCE -> mapType(type.elementType().orElseThrow(), modules) + "[]";
      };
    }

    private String mapType(IdlTypeReference type, IdlDeclarator declarator, List<String> modules) {
      return appendArrayDimensions(mapType(type, modules), declarator.dimensions().size());
    }

    private String mapType(String idlType, List<String> modules) {
      if (idlType.startsWith("sequence<")) {
        return "java.lang.Object[]";
      }
      if (idlType.startsWith("string<") || idlType.startsWith("wstring<")) {
        return "java.lang.String";
      }
      return switch (idlType) {
        case "void" -> "void";
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
        default -> mapUserType(idlType, modules);
      };
    }

    private String mapUserType(String idlType, List<String> modules) {
      IdlSymbol symbol = resolveSymbol(idlType, modules);
      if (symbol.kind() == IdlSymbolKind.TYPEDEF) {
        return mappedAliasType(symbol);
      }
      if (!Set.of(
              IdlSymbolKind.STRUCT,
              IdlSymbolKind.ENUM,
              IdlSymbolKind.EXCEPTION,
              IdlSymbolKind.INTERFACE,
              IdlSymbolKind.UNION)
          .contains(symbol.kind())) {
        throw new IllegalArgumentException("IDL type is not a generated Java type: " + idlType);
      }
      List<String> idlParts = splitAbsolute(symbol.qualifiedName());
      return mappedTypeName(idlParts.getLast(), idlParts.subList(0, idlParts.size() - 1))
          .qualifiedName();
    }

    private String holderType(IdlTypeReference type, List<String> modules) {
      String name =
          type.kind() == io.github.mundanej.mjo.idl.ast.IdlTypeReferenceKind.NAMED
              ? holderType(type.name(), modules)
              : syntheticHolder(type.name() + "Holder", mapType(type, modules), modules);
      return name;
    }

    private String holderType(String idlType, List<String> modules) {
      if (primitiveJavaType(idlType).isPresent()) {
        return syntheticHolder(
            typeName(idlType) + "Holder", primitiveJavaType(idlType).orElseThrow(), modules);
      }
      IdlSymbol symbol = resolveSymbol(idlType, modules);
      if (symbol.kind() == IdlSymbolKind.TYPEDEF
          || symbol.kind() == IdlSymbolKind.STRUCT
          || symbol.kind() == IdlSymbolKind.ENUM
          || symbol.kind() == IdlSymbolKind.EXCEPTION
          || symbol.kind() == IdlSymbolKind.INTERFACE
          || symbol.kind() == IdlSymbolKind.UNION) {
        List<String> idlParts = splitAbsolute(symbol.qualifiedName());
        JavaMappedName mapped =
            mappedTypeName(idlParts.getLast(), idlParts.subList(0, idlParts.size() - 1));
        return new JavaMappedName(mapped.packageName(), mapped.simpleName() + "Holder")
            .qualifiedName();
      }
      return syntheticHolder(typeName(idlType) + "Holder", mapType(idlType, modules), modules);
    }

    private String syntheticHolder(String baseName, String valueType, List<String> modules) {
      JavaMappedName holderName = new JavaMappedName(packageName(modules), typeName(baseName));
      if (syntheticHolderNames.add(holderName.qualifiedName())) {
        syntheticHolders.add(
            new JavaMappedType(
                JavaMappedTypeKind.HOLDER,
                holderName,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                valueType));
      }
      return holderName.qualifiedName();
    }

    private static String appendArrayDimensions(String javaType, int dimensions) {
      return javaType + "[]".repeat(Math.max(0, dimensions));
    }

    private static Optional<String> primitiveJavaType(String idlType) {
      return switch (idlType) {
        case "void" -> Optional.of("void");
        case "boolean" -> Optional.of("boolean");
        case "char", "wchar" -> Optional.of("char");
        case "octet", "uint8" -> Optional.of("short");
        case "short", "int16" -> Optional.of("short");
        case "unsigned short", "uint16" -> Optional.of("int");
        case "long", "int32" -> Optional.of("int");
        case "unsigned long", "uint32" -> Optional.of("long");
        case "long long", "int64" -> Optional.of("long");
        case "unsigned long long", "uint64" -> Optional.of("java.math.BigInteger");
        case "float" -> Optional.of("float");
        case "double" -> Optional.of("double");
        case "long double" -> Optional.of("java.math.BigDecimal");
        case "string", "wstring" -> Optional.of("java.lang.String");
        case "any", "Object", "ValueBase" -> Optional.of("java.lang.Object");
        default -> Optional.empty();
      };
    }

    private IdlSymbol resolveSymbol(String idlName, List<String> modules) {
      if (idlName.startsWith("::")) {
        return symbol(idlName);
      }
      for (int count = modules.size(); count >= 0; count--) {
        List<String> prefix = modules.subList(0, count);
        Optional<IdlSymbol> resolved = semanticModel.findSymbol(absoluteName(prefix, idlName));
        if (resolved.isPresent()) {
          return resolved.orElseThrow();
        }
      }
      throw new IllegalArgumentException("Unresolved IDL name in valid semantic model: " + idlName);
    }

    private String mappedAliasType(IdlSymbol symbol) {
      List<String> idlParts = splitAbsolute(symbol.qualifiedName());
      String javaName =
          mappedTypeName(idlParts.getLast(), idlParts.subList(0, idlParts.size() - 1))
              .qualifiedName();
      return types.stream()
          .filter(type -> type.name().qualifiedName().equals(javaName))
          .map(JavaMappedType::aliasType)
          .findFirst()
          .orElseGet(() -> mapType(symbol.resolvedTypeName().orElseThrow(), List.of()));
    }

    private IdlSymbol symbol(String qualifiedName) {
      return semanticModel
          .findSymbol(qualifiedName)
          .orElseThrow(
              () ->
                  new IllegalArgumentException(
                      "Missing symbol in valid semantic model: " + qualifiedName));
    }

    private JavaMappedName mappedTypeName(String idlName, List<String> modules) {
      return new JavaMappedName(packageName(modules), typeName(idlName));
    }

    private String packageName(List<String> modules) {
      List<String> segments = new ArrayList<>();
      if (mode == JavaMappingMode.MODERN) {
        segments.add("modern");
      }
      segments.addAll(modules.stream().map(IdlJavaMapper::packageSegment).toList());
      return String.join(".", segments);
    }
  }

  private static String absoluteName(List<String> modules, String name) {
    String normalized = name.startsWith("::") ? name.substring(2) : name;
    if (modules.isEmpty()) {
      return "::" + normalized;
    }
    return "::" + String.join("::", modules) + "::" + normalized;
  }

  private static List<String> splitAbsolute(String qualifiedName) {
    String normalized = qualifiedName.startsWith("::") ? qualifiedName.substring(2) : qualifiedName;
    return List.of(normalized.split("::"));
  }

  private static String typeName(String idlName) {
    return escapeJavaIdentifier(toUpperCamel(idlName));
  }

  private static String memberName(String idlName) {
    return escapeJavaIdentifier(toLowerCamel(idlName));
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
    return escapeJavaIdentifier(result.toString());
  }

  private static String packageSegment(String idlName) {
    return escapeJavaIdentifier(sanitize(idlName).toLowerCase(Locale.ROOT));
  }

  private static String toUpperCamel(String value) {
    String sanitized = sanitize(value);
    return Character.toUpperCase(sanitized.charAt(0)) + sanitized.substring(1);
  }

  private static String toLowerCamel(String value) {
    String sanitized = sanitize(value);
    return Character.toLowerCase(sanitized.charAt(0)) + sanitized.substring(1);
  }

  private static String sanitize(String value) {
    StringBuilder result = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if ((index == 0 && Character.isJavaIdentifierStart(character))
          || (index > 0 && Character.isJavaIdentifierPart(character))) {
        result.append(character);
      } else if (index == 0) {
        result.append('_');
        if (Character.isJavaIdentifierPart(character)) {
          result.append(character);
        }
      } else {
        result.append('_');
      }
    }
    if (result.isEmpty()) {
      return "_";
    }
    return result.toString();
  }

  private static String escapeJavaIdentifier(String value) {
    return JAVA_KEYWORDS.contains(value) ? value + "_" : value;
  }

  private static String initializer(JavaMappingMode mode, String javaType, IdlConstantValue value) {
    return switch (value) {
      case IdlConstantValue.IntegerValue integer -> integerInitializer(javaType, integer.value());
      case IdlConstantValue.FloatingValue floating ->
          floatingInitializer(javaType, floating.value());
      case IdlConstantValue.BooleanValue bool -> Boolean.toString(bool.value());
      case IdlConstantValue.CharacterValue character -> characterInitializer(character.value());
      case IdlConstantValue.StringValue string -> stringInitializer(string.value());
      case IdlConstantValue.EnumeratorValue enumerator -> enumInitializer(mode, enumerator);
    };
  }

  private static String integerInitializer(String javaType, BigInteger value) {
    return switch (javaType) {
      case "byte" -> "(byte) " + value;
      case "short" -> "(short) " + value;
      case "int" -> value.toString();
      case "long" -> value + "L";
      case "java.math.BigInteger" -> "new java.math.BigInteger(\"" + value + "\")";
      default -> value.toString();
    };
  }

  private static String floatingInitializer(String javaType, BigDecimal value) {
    return switch (javaType) {
      case "float" -> value.toPlainString() + "f";
      case "double" -> value.toPlainString() + "d";
      case "java.math.BigDecimal" -> "new java.math.BigDecimal(\"" + value.toPlainString() + "\")";
      default -> value.toPlainString();
    };
  }

  private static String characterInitializer(String value) {
    return "'" + escapeChar(value.charAt(0)) + "'";
  }

  private static String stringInitializer(String value) {
    StringBuilder result = new StringBuilder("\"");
    for (int index = 0; index < value.length(); index++) {
      result.append(escapeChar(value.charAt(index)));
    }
    return result.append('"').toString();
  }

  private static String enumInitializer(
      JavaMappingMode mode, IdlConstantValue.EnumeratorValue value) {
    List<String> typeParts = splitAbsolute(value.idlType());
    List<String> enumParts = splitAbsolute(value.enumeratorName());
    List<String> packageParts = new ArrayList<>();
    if (mode == JavaMappingMode.MODERN) {
      packageParts.add("modern");
    }
    if (typeParts.size() > 1) {
      packageParts.addAll(
          typeParts.subList(0, typeParts.size() - 1).stream()
              .map(IdlJavaMapper::packageSegment)
              .toList());
    }
    String packageName = String.join(".", packageParts);
    String enumType =
        new JavaMappedName(packageName, typeName(typeParts.getLast())).qualifiedName();
    return enumType + "." + constantName(enumParts.getLast());
  }

  private static String escapeChar(char character) {
    return switch (character) {
      case '\b' -> "\\b";
      case '\t' -> "\\t";
      case '\n' -> "\\n";
      case '\f' -> "\\f";
      case '\r' -> "\\r";
      case '"' -> "\\\"";
      case '\'' -> "\\'";
      case '\\' -> "\\\\";
      default ->
          Character.isISOControl(character)
              ? String.format("\\u%04X", (int) character)
              : Character.toString(character);
    };
  }
}
