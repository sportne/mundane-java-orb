package io.github.mundanej.mjo.idl.java.mapping;

import io.github.mundanej.mjo.idl.ast.IdlAttribute;
import io.github.mundanej.mjo.idl.ast.IdlConstant;
import io.github.mundanej.mjo.idl.ast.IdlDeclaration;
import io.github.mundanej.mjo.idl.ast.IdlEnum;
import io.github.mundanej.mjo.idl.ast.IdlExceptionDeclaration;
import io.github.mundanej.mjo.idl.ast.IdlField;
import io.github.mundanej.mjo.idl.ast.IdlInterface;
import io.github.mundanej.mjo.idl.ast.IdlInterfaceMember;
import io.github.mundanej.mjo.idl.ast.IdlModule;
import io.github.mundanej.mjo.idl.ast.IdlOperation;
import io.github.mundanej.mjo.idl.ast.IdlParameter;
import io.github.mundanej.mjo.idl.ast.IdlStruct;
import io.github.mundanej.mjo.idl.semantics.IdlConstantValue;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticModel;
import io.github.mundanej.mjo.idl.semantics.IdlSymbol;
import io.github.mundanej.mjo.idl.semantics.IdlSymbolKind;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
    private final List<JavaMappedConstantScope> constantScopes = new ArrayList<>();

    private Mapper(IdlSemanticModel semanticModel, JavaMappingMode mode) {
      this.semanticModel = semanticModel;
      this.mode = mode;
    }

    private JavaMappingModel map() {
      mapDeclarations(semanticModel.translationUnit().declarations(), List.of());
      return new JavaMappingModel(
          mode, semanticModel.translationUnit().span().start().sourceName(), types, constantScopes);
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
      } else if (declaration instanceof IdlInterface idlInterface) {
        types.add(mapInterface(idlInterface, modules));
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
          attributes);
    }

    private JavaMappedField mapField(IdlField field, List<String> modules) {
      return new JavaMappedField(mapType(field.type().name(), modules), memberName(field.name()));
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
      return new JavaMappedParameter(
          mapType(parameter.type().name(), modules), memberName(parameter.name()));
    }

    private List<JavaMappedAttribute> mapAttribute(IdlAttribute attribute, List<String> modules) {
      String javaType = mapType(attribute.type().name(), modules);
      return attribute.names().stream()
          .map(name -> new JavaMappedAttribute(javaType, memberName(name), attribute.readonly()))
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

    private String mapType(String idlType, List<String> modules) {
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
      if (!Set.of(
              IdlSymbolKind.STRUCT,
              IdlSymbolKind.ENUM,
              IdlSymbolKind.EXCEPTION,
              IdlSymbolKind.INTERFACE)
          .contains(symbol.kind())) {
        throw new IllegalArgumentException("IDL type is not a generated Java type: " + idlType);
      }
      List<String> idlParts = splitAbsolute(symbol.qualifiedName());
      return mappedTypeName(idlParts.getLast(), idlParts.subList(0, idlParts.size() - 1))
          .qualifiedName();
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
