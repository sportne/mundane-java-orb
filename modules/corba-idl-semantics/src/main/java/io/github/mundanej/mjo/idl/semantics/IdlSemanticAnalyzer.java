package io.github.mundanej.mjo.idl.semantics;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import io.github.mundanej.mjo.common.SourceSpan;
import io.github.mundanej.mjo.idl.ast.IdlAttribute;
import io.github.mundanej.mjo.idl.ast.IdlConstant;
import io.github.mundanej.mjo.idl.ast.IdlConstantExpression;
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
import io.github.mundanej.mjo.idl.ast.IdlStruct;
import io.github.mundanej.mjo.idl.ast.IdlTranslationUnit;
import io.github.mundanej.mjo.idl.ast.IdlTypeReference;
import io.github.mundanej.mjo.idl.ast.IdlTypeReferenceKind;
import io.github.mundanej.mjo.idl.ast.IdlTypedef;
import io.github.mundanej.mjo.idl.ast.IdlUnion;
import io.github.mundanej.mjo.idl.ast.IdlUnionCase;
import io.github.mundanej.mjo.idl.ast.IdlUnionLabel;
import io.github.mundanej.mjo.idl.ast.IdlValueBox;
import io.github.mundanej.mjo.idl.ast.IdlValueFactory;
import io.github.mundanej.mjo.idl.ast.IdlValueField;
import io.github.mundanej.mjo.idl.ast.IdlValueMember;
import io.github.mundanej.mjo.idl.ast.IdlValueType;
import io.github.mundanej.mjo.idl.ast.IdlValueTypeForward;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Semantic analyzer for the approved minimal OMG IDL parser slice. */
public final class IdlSemanticAnalyzer {

  /** Creates a stateless semantic analyzer. */
  public IdlSemanticAnalyzer() {}

  /** Analyzes one syntax-only IDL translation unit. */
  public IdlSemanticResult analyze(IdlTranslationUnit translationUnit) {
    return new Analyzer(translationUnit).analyze();
  }

  private static final class Analyzer {

    private static final Set<String> BUILTIN_TYPES =
        Set.of(
            "any",
            "boolean",
            "char",
            "double",
            "float",
            "int8",
            "int16",
            "int32",
            "int64",
            "long",
            "long double",
            "long long",
            "Object",
            "octet",
            "short",
            "string",
            "uint8",
            "uint16",
            "uint32",
            "uint64",
            "unsigned long",
            "unsigned long long",
            "unsigned short",
            "ValueBase",
            "wchar",
            "wstring");
    private static final Set<String> INTEGER_CONSTANT_TYPES =
        Set.of(
            "short",
            "unsigned short",
            "long",
            "unsigned long",
            "long long",
            "unsigned long long",
            "int8",
            "uint8",
            "int16",
            "uint16",
            "int32",
            "uint32",
            "int64",
            "uint64",
            "octet");
    private static final Set<String> FLOATING_CONSTANT_TYPES =
        Set.of("float", "double", "long double");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("_?[A-Za-z][A-Za-z0-9_]*");

    private final IdlTranslationUnit translationUnit;
    private final Scope globalScope = new Scope(null, "");
    private final List<MutableSymbol> symbols = new ArrayList<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final IdentityHashMap<IdlModule, Scope> moduleScopes = new IdentityHashMap<>();
    private final IdentityHashMap<IdlInterface, Scope> interfaceScopes = new IdentityHashMap<>();
    private final IdentityHashMap<IdlOperation, Scope> operationScopes = new IdentityHashMap<>();
    private final IdentityHashMap<IdlStruct, Scope> structScopes = new IdentityHashMap<>();
    private final IdentityHashMap<IdlUnion, Scope> unionScopes = new IdentityHashMap<>();
    private final IdentityHashMap<IdlExceptionDeclaration, Scope> exceptionScopes =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlEnum, Scope> enumScopes = new IdentityHashMap<>();
    private final IdentityHashMap<IdlInterfaceForward, MutableSymbol> interfaceForwardSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlInterface, MutableSymbol> interfaceSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlValueTypeForward, MutableSymbol> valueForwardSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlValueType, Scope> valueTypeScopes = new IdentityHashMap<>();
    private final IdentityHashMap<IdlValueType, MutableSymbol> valueTypeSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlValueBox, MutableSymbol> valueBoxSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlNative, MutableSymbol> nativeSymbols = new IdentityHashMap<>();
    private final IdentityHashMap<IdlTypedef, List<MutableSymbol>> typedefSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlConstant, MutableSymbol> constantSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlField, MutableSymbol> fieldSymbols = new IdentityHashMap<>();
    private final IdentityHashMap<IdlUnionCase, MutableSymbol> unionCaseSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlOperation, MutableSymbol> operationSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlParameter, MutableSymbol> parameterSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlAttribute, List<MutableSymbol>> attributeSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlEnum, List<MutableSymbol>> enumeratorSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlValueField, List<MutableSymbol>> valueFieldSymbols =
        new IdentityHashMap<>();
    private final IdentityHashMap<IdlValueFactory, MutableSymbol> valueFactorySymbols =
        new IdentityHashMap<>();
    private final Map<MutableSymbol, List<MutableSymbol>> interfaceBaseGraph = new HashMap<>();
    private final List<MutableSymbol> interfaceBaseValidationOrder = new ArrayList<>();
    private int nextOrder;

    private Analyzer(IdlTranslationUnit translationUnit) {
      this.translationUnit = Objects.requireNonNull(translationUnit, "translationUnit");
    }

    private IdlSemanticResult analyze() {
      collectDeclarations(translationUnit.declarations(), globalScope);
      validateDeclarations(translationUnit.declarations(), globalScope, new HashMap<>());
      validateInterfaceCycles();
      if (hasErrors()) {
        return new IdlSemanticResult(Optional.empty(), diagnostics);
      }
      return new IdlSemanticResult(
          Optional.of(new IdlSemanticModel(translationUnit, freezeSymbols())), diagnostics);
    }

    private void collectDeclarations(List<IdlDeclaration> declarations, Scope scope) {
      for (IdlDeclaration declaration : declarations) {
        collectDeclaration(declaration, scope);
      }
    }

    private void collectDeclaration(IdlDeclaration declaration, Scope scope) {
      if (declaration instanceof IdlModule module) {
        MutableSymbol symbol =
            define(scope, IdlSymbolKind.MODULE, module.name(), Optional.empty(), module.span());
        if (symbol != null) {
          Scope moduleScope = new Scope(scope, symbol.qualifiedName);
          symbol.childScope = moduleScope;
          moduleScopes.put(module, moduleScope);
          collectDeclarations(module.declarations(), moduleScope);
        }
      } else if (declaration instanceof IdlStruct struct) {
        collectStruct(struct, scope);
      } else if (declaration instanceof IdlUnion union) {
        collectUnion(union, scope);
      } else if (declaration instanceof IdlTypedef typedef) {
        collectTypedef(typedef, scope);
      } else if (declaration instanceof IdlEnum idlEnum) {
        collectEnum(idlEnum, scope);
      } else if (declaration instanceof IdlExceptionDeclaration exception) {
        collectException(exception, scope);
      } else if (declaration instanceof IdlConstant constant) {
        MutableSymbol symbol =
            define(
                scope,
                IdlSymbolKind.CONSTANT,
                constant.name(),
                Optional.of(constant.type().name()),
                constant.span());
        if (symbol != null) {
          constantSymbols.put(constant, symbol);
        }
      } else if (declaration instanceof IdlInterface idlInterface) {
        collectInterface(idlInterface, scope);
      } else if (declaration instanceof IdlInterfaceForward forward) {
        MutableSymbol symbol = defineInterface(scope, forward.name(), forward.span(), true);
        if (symbol != null) {
          interfaceForwardSymbols.put(forward, symbol);
        }
      } else if (declaration instanceof IdlNative nativeDeclaration) {
        MutableSymbol symbol =
            define(
                scope,
                IdlSymbolKind.NATIVE,
                nativeDeclaration.name(),
                Optional.empty(),
                nativeDeclaration.span());
        if (symbol != null) {
          nativeSymbols.put(nativeDeclaration, symbol);
        }
      } else if (declaration instanceof IdlValueBox valueBox) {
        MutableSymbol symbol =
            define(
                scope,
                IdlSymbolKind.VALUE_BOX,
                valueBox.name(),
                Optional.of(valueBox.boxedType().name()),
                valueBox.span());
        if (symbol != null) {
          valueBoxSymbols.put(valueBox, symbol);
        }
      } else if (declaration instanceof IdlValueType valueType) {
        collectValueType(valueType, scope);
      } else if (declaration instanceof IdlValueTypeForward forward) {
        MutableSymbol symbol = defineValueType(scope, forward.name(), forward.span(), true);
        if (symbol != null) {
          valueForwardSymbols.put(forward, symbol);
        }
      }
    }

    private void collectValueType(IdlValueType valueType, Scope scope) {
      MutableSymbol symbol = defineValueType(scope, valueType.name(), valueType.span(), false);
      if (symbol == null) {
        return;
      }
      Scope valueScope = new Scope(scope, symbol.qualifiedName);
      symbol.childScope = valueScope;
      valueTypeSymbols.put(valueType, symbol);
      valueTypeScopes.put(valueType, valueScope);
      for (IdlValueMember member : valueType.members()) {
        collectValueMember(member, valueScope);
      }
    }

    private void collectValueMember(IdlValueMember member, Scope scope) {
      if (member instanceof IdlValueField field) {
        List<MutableSymbol> symbolsForField = new ArrayList<>();
        for (IdlDeclarator declarator : field.declarators()) {
          MutableSymbol symbol =
              define(
                  scope,
                  IdlSymbolKind.VALUE_FIELD,
                  declarator.name(),
                  Optional.of(field.type().name()),
                  declarator.span());
          if (symbol != null) {
            symbolsForField.add(symbol);
          }
        }
        valueFieldSymbols.put(field, symbolsForField);
      } else if (member instanceof IdlValueFactory factory) {
        MutableSymbol symbol =
            define(
                scope,
                IdlSymbolKind.VALUE_FACTORY,
                factory.name(),
                Optional.empty(),
                factory.span());
        if (symbol != null) {
          valueFactorySymbols.put(factory, symbol);
          Scope factoryScope = new Scope(scope, symbol.qualifiedName);
          symbol.childScope = factoryScope;
          for (IdlParameter parameter : factory.parameters()) {
            MutableSymbol parameterSymbol =
                define(
                    factoryScope,
                    IdlSymbolKind.PARAMETER,
                    parameter.name(),
                    Optional.of(parameter.type().name()),
                    parameter.span());
            if (parameterSymbol != null) {
              parameterSymbols.put(parameter, parameterSymbol);
            }
          }
        }
      } else if (member instanceof IdlInterfaceMember interfaceMember) {
        collectInterfaceMember(interfaceMember, scope);
      }
    }

    private void collectStruct(IdlStruct struct, Scope scope) {
      MutableSymbol symbol =
          define(scope, IdlSymbolKind.STRUCT, struct.name(), Optional.empty(), struct.span());
      if (symbol == null) {
        return;
      }
      Scope structScope = new Scope(scope, symbol.qualifiedName);
      symbol.childScope = structScope;
      structScopes.put(struct, structScope);
      for (IdlField field : struct.fields()) {
        MutableSymbol fieldSymbol =
            define(
                structScope,
                IdlSymbolKind.FIELD,
                field.name(),
                Optional.of(field.type().name()),
                field.span());
        if (fieldSymbol != null) {
          fieldSymbols.put(field, fieldSymbol);
        }
      }
    }

    private void collectUnion(IdlUnion union, Scope scope) {
      MutableSymbol symbol =
          define(scope, IdlSymbolKind.UNION, union.name(), Optional.empty(), union.span());
      if (symbol == null) {
        return;
      }
      Scope unionScope = new Scope(scope, symbol.qualifiedName);
      symbol.childScope = unionScope;
      unionScopes.put(union, unionScope);
      for (IdlUnionCase unionCase : union.cases()) {
        MutableSymbol fieldSymbol =
            define(
                unionScope,
                IdlSymbolKind.FIELD,
                unionCase.declarator().name(),
                Optional.of(unionCase.type().name()),
                unionCase.declarator().span());
        if (fieldSymbol != null) {
          unionCaseSymbols.put(unionCase, fieldSymbol);
        }
      }
    }

    private void collectTypedef(IdlTypedef typedef, Scope scope) {
      List<MutableSymbol> symbolsForTypedef = new ArrayList<>();
      for (IdlDeclarator declarator : typedef.declarators()) {
        MutableSymbol symbol =
            define(
                scope,
                IdlSymbolKind.TYPEDEF,
                declarator.name(),
                Optional.of(typedef.type().name()),
                declarator.span());
        if (symbol != null) {
          symbolsForTypedef.add(symbol);
        }
      }
      typedefSymbols.put(typedef, symbolsForTypedef);
    }

    private void collectEnum(IdlEnum idlEnum, Scope scope) {
      MutableSymbol symbol =
          define(scope, IdlSymbolKind.ENUM, idlEnum.name(), Optional.empty(), idlEnum.span());
      if (symbol == null) {
        return;
      }
      Scope enumScope = new Scope(scope, symbol.qualifiedName);
      symbol.childScope = enumScope;
      enumScopes.put(idlEnum, enumScope);
      List<MutableSymbol> enumerators = new ArrayList<>();
      for (String enumerator : idlEnum.enumerators()) {
        MutableSymbol enumeratorSymbol =
            define(
                enumScope, IdlSymbolKind.ENUMERATOR, enumerator, Optional.empty(), idlEnum.span());
        if (enumeratorSymbol != null) {
          enumerators.add(enumeratorSymbol);
        }
      }
      enumeratorSymbols.put(idlEnum, enumerators);
    }

    private void collectException(IdlExceptionDeclaration exception, Scope scope) {
      MutableSymbol symbol =
          define(
              scope, IdlSymbolKind.EXCEPTION, exception.name(), Optional.empty(), exception.span());
      if (symbol == null) {
        return;
      }
      Scope exceptionScope = new Scope(scope, symbol.qualifiedName);
      symbol.childScope = exceptionScope;
      exceptionScopes.put(exception, exceptionScope);
      for (IdlField field : exception.fields()) {
        MutableSymbol fieldSymbol =
            define(
                exceptionScope,
                IdlSymbolKind.FIELD,
                field.name(),
                Optional.of(field.type().name()),
                field.span());
        if (fieldSymbol != null) {
          fieldSymbols.put(field, fieldSymbol);
        }
      }
    }

    private void collectInterface(IdlInterface idlInterface, Scope scope) {
      MutableSymbol symbol =
          defineInterface(scope, idlInterface.name(), idlInterface.span(), false);
      if (symbol == null) {
        return;
      }
      Scope interfaceScope = new Scope(scope, symbol.qualifiedName);
      symbol.childScope = interfaceScope;
      interfaceSymbols.put(idlInterface, symbol);
      interfaceScopes.put(idlInterface, interfaceScope);
      for (IdlInterfaceMember member : idlInterface.members()) {
        collectInterfaceMember(member, interfaceScope);
      }
    }

    private void collectInterfaceMember(IdlInterfaceMember member, Scope scope) {
      if (member instanceof IdlAttribute attribute) {
        List<MutableSymbol> symbolsForAttribute = new ArrayList<>();
        for (String name : attribute.names()) {
          MutableSymbol symbol =
              define(
                  scope,
                  IdlSymbolKind.ATTRIBUTE,
                  name,
                  Optional.of(attribute.type().name()),
                  attribute.span());
          if (symbol != null) {
            symbolsForAttribute.add(symbol);
          }
        }
        attributeSymbols.put(attribute, symbolsForAttribute);
      } else if (member instanceof IdlOperation operation) {
        MutableSymbol symbol =
            define(
                scope,
                IdlSymbolKind.OPERATION,
                operation.name(),
                Optional.of(operation.returnType().name()),
                operation.span());
        if (symbol == null) {
          return;
        }
        operationSymbols.put(operation, symbol);
        Scope operationScope = new Scope(scope, symbol.qualifiedName);
        symbol.childScope = operationScope;
        operationScopes.put(operation, operationScope);
        for (IdlParameter parameter : operation.parameters()) {
          MutableSymbol parameterSymbol =
              define(
                  operationScope,
                  IdlSymbolKind.PARAMETER,
                  parameter.name(),
                  Optional.of(parameter.type().name()),
                  parameter.span());
          if (parameterSymbol != null) {
            parameterSymbols.put(parameter, parameterSymbol);
          }
        }
      }
    }

    private MutableSymbol define(
        Scope scope, IdlSymbolKind kind, String name, Optional<String> typeName, SourceSpan span) {
      String key = key(name);
      MutableSymbol existing = scope.symbolsByLowerName.get(key);
      if (existing != null) {
        emit(
            IdlSemanticDiagnosticCodes.DUPLICATE_NAME,
            "Duplicate IDL name in scope: " + name,
            span);
        return null;
      }
      MutableSymbol symbol =
          new MutableSymbol(
              kind, name, scope.childQualifiedName(name), typeName, span, nextOrder++);
      scope.symbolsByLowerName.put(key, symbol);
      symbols.add(symbol);
      return symbol;
    }

    private MutableSymbol defineInterface(
        Scope scope, String name, SourceSpan span, boolean forwardDeclaration) {
      String key = key(name);
      MutableSymbol existing = scope.symbolsByLowerName.get(key);
      if (existing == null) {
        return define(scope, IdlSymbolKind.INTERFACE, name, Optional.empty(), span);
      }
      if (!existing.name.equals(name)) {
        emit(
            IdlSemanticDiagnosticCodes.DUPLICATE_NAME,
            "Duplicate IDL name in scope: " + name,
            span);
        return null;
      }
      if (existing.kind == IdlSymbolKind.INTERFACE
          && (forwardDeclaration || existing.childScope == null)) {
        return existing;
      }
      emit(IdlSemanticDiagnosticCodes.DUPLICATE_NAME, "Duplicate IDL name in scope: " + name, span);
      return null;
    }

    private MutableSymbol defineValueType(
        Scope scope, String name, SourceSpan span, boolean forwardDeclaration) {
      String key = key(name);
      MutableSymbol existing = scope.symbolsByLowerName.get(key);
      if (existing == null) {
        return define(scope, IdlSymbolKind.VALUETYPE, name, Optional.empty(), span);
      }
      if (!existing.name.equals(name)) {
        emit(
            IdlSemanticDiagnosticCodes.DUPLICATE_NAME,
            "Duplicate IDL name in scope: " + name,
            span);
        return null;
      }
      if (existing.kind == IdlSymbolKind.VALUETYPE
          && (forwardDeclaration || existing.childScope == null)) {
        return existing;
      }
      emit(IdlSemanticDiagnosticCodes.DUPLICATE_NAME, "Duplicate IDL name in scope: " + name, span);
      return null;
    }

    private void validateDeclarations(
        List<IdlDeclaration> declarations,
        Scope scope,
        Map<String, MutableSymbol> availableValues) {
      for (IdlDeclaration declaration : declarations) {
        validateDeclaration(declaration, scope, availableValues);
      }
    }

    private void validateDeclaration(
        IdlDeclaration declaration, Scope scope, Map<String, MutableSymbol> availableValues) {
      if (declaration instanceof IdlModule module) {
        Scope moduleScope = moduleScopes.get(module);
        if (moduleScope != null) {
          validateDeclarations(module.declarations(), moduleScope, availableValues);
        }
      } else if (declaration instanceof IdlStruct struct) {
        validateFields(struct.fields(), structScopes.get(struct), availableValues);
      } else if (declaration instanceof IdlUnion union) {
        validateUnion(union, unionScopes.get(union), availableValues);
      } else if (declaration instanceof IdlTypedef typedef) {
        validateTypedef(typedef, scope, availableValues);
      } else if (declaration instanceof IdlEnum idlEnum) {
        validateEnum(idlEnum, availableValues);
      } else if (declaration instanceof IdlExceptionDeclaration exception) {
        validateFields(exception.fields(), exceptionScopes.get(exception), availableValues);
      } else if (declaration instanceof IdlConstant constant) {
        validateConstant(constant, scope, availableValues);
      } else if (declaration instanceof IdlInterface idlInterface) {
        Scope interfaceScope = interfaceScopes.get(idlInterface);
        if (interfaceScope != null) {
          validateInterfaceInheritance(idlInterface, scope);
          for (IdlInterfaceMember member : idlInterface.members()) {
            validateInterfaceMember(member, interfaceScope, availableValues);
          }
        }
      } else if (declaration instanceof IdlValueBox valueBox) {
        validateValueBox(valueBox, scope, availableValues);
      } else if (declaration instanceof IdlValueType valueType) {
        validateValueType(valueType, scope, availableValues);
      }
    }

    private void validateValueBox(
        IdlValueBox valueBox, Scope scope, Map<String, MutableSymbol> availableValues) {
      ResolvedType boxedType = resolveType(valueBox.boxedType(), scope, false, availableValues);
      MutableSymbol symbol = valueBoxSymbols.get(valueBox);
      if (symbol != null && boxedType != null) {
        symbol.resolvedTypeName = Optional.of(boxedType.name());
      }
    }

    private void validateValueType(
        IdlValueType valueType, Scope scope, Map<String, MutableSymbol> availableValues) {
      Scope valueScope = valueTypeScopes.get(valueType);
      if (valueScope == null) {
        return;
      }
      for (String baseName : valueType.baseValueTypes()) {
        Optional<MutableSymbol> resolved =
            resolveName(
                scope, baseName, valueType.span(), IdlSemanticDiagnosticCodes.UNRESOLVED_NAME);
        if (resolved.isPresent() && resolved.orElseThrow().kind != IdlSymbolKind.VALUETYPE) {
          emit(
              IdlSemanticDiagnosticCodes.INVALID_INHERITANCE,
              "Valuetype base must resolve to a valuetype: " + baseName,
              valueType.span());
        }
      }
      for (String supportedName : valueType.supportedInterfaces()) {
        Optional<MutableSymbol> resolved =
            resolveName(
                scope, supportedName, valueType.span(), IdlSemanticDiagnosticCodes.UNRESOLVED_NAME);
        if (resolved.isPresent() && resolved.orElseThrow().kind != IdlSymbolKind.INTERFACE) {
          emit(
              IdlSemanticDiagnosticCodes.INVALID_INHERITANCE,
              "Valuetype supports target must resolve to an interface: " + supportedName,
              valueType.span());
        }
      }
      for (IdlValueMember member : valueType.members()) {
        validateValueMember(member, valueScope, availableValues);
      }
    }

    private void validateValueMember(
        IdlValueMember member, Scope valueScope, Map<String, MutableSymbol> availableValues) {
      if (member instanceof IdlValueField field) {
        ResolvedType resolvedType =
            resolveType(field.type(), valueScope.parent, false, availableValues);
        for (IdlDeclarator declarator : field.declarators()) {
          validateDeclaratorDimensions(declarator, valueScope.parent, availableValues);
        }
        if (resolvedType != null) {
          for (MutableSymbol symbol : valueFieldSymbols.getOrDefault(field, List.of())) {
            symbol.resolvedTypeName = Optional.of(resolvedType.name());
          }
        }
      } else if (member instanceof IdlValueFactory factory) {
        validateValueFactory(factory, valueScope, availableValues);
      } else if (member instanceof IdlInterfaceMember interfaceMember) {
        validateInterfaceMember(interfaceMember, valueScope, availableValues);
      }
    }

    private void validateValueFactory(
        IdlValueFactory factory, Scope valueScope, Map<String, MutableSymbol> availableValues) {
      Scope typeLookupScope = valueScope.parent;
      for (IdlParameter parameter : factory.parameters()) {
        ResolvedType parameterType =
            resolveType(parameter.type(), typeLookupScope, false, availableValues);
        MutableSymbol parameterSymbol = parameterSymbols.get(parameter);
        if (parameterSymbol != null && parameterType != null) {
          parameterSymbol.resolvedTypeName = Optional.of(parameterType.name());
        }
      }
      MutableSymbol factorySymbol = valueFactorySymbols.get(factory);
      for (String raisesName : factory.raises()) {
        validateRaisesTarget(raisesName, factory.span(), factorySymbol, typeLookupScope);
      }
    }

    private void validateFields(
        List<IdlField> fields, Scope scope, Map<String, MutableSymbol> availableValues) {
      if (scope == null) {
        return;
      }
      Scope typeLookupScope = scope.parent;
      for (IdlField field : fields) {
        ResolvedType resolvedType =
            resolveType(field.type(), typeLookupScope, false, availableValues);
        validateDeclaratorDimensions(field.declarator(), typeLookupScope, availableValues);
        MutableSymbol symbol = fieldSymbols.get(field);
        if (symbol != null && resolvedType != null) {
          symbol.resolvedTypeName = Optional.of(resolvedType.name());
        }
      }
    }

    private void validateEnum(IdlEnum idlEnum, Map<String, MutableSymbol> availableValues) {
      MutableSymbol enumSymbol = resolveDefinedSymbol(enumScopes.get(idlEnum));
      if (enumSymbol == null) {
        return;
      }
      for (MutableSymbol enumerator : enumeratorSymbols.getOrDefault(idlEnum, List.of())) {
        enumerator.resolvedTypeName = Optional.of(enumSymbol.qualifiedName);
        enumerator.constantValue =
            Optional.of(
                IdlConstantValue.enumerator(enumSymbol.qualifiedName, enumerator.qualifiedName));
        availableValues.put(enumerator.qualifiedName, enumerator);
      }
    }

    private void validateConstant(
        IdlConstant constant, Scope scope, Map<String, MutableSymbol> availableValues) {
      MutableSymbol symbol = constantSymbols.get(constant);
      ResolvedType resolvedType = resolveType(constant.type(), scope, false, availableValues);
      if (symbol != null && resolvedType != null) {
        symbol.resolvedTypeName = Optional.of(resolvedType.name());
      }
      if (symbol == null || resolvedType == null) {
        return;
      }
      Optional<IdlConstantValue> value =
          evaluateConstant(constant, resolvedType, scope, availableValues);
      value.ifPresent(
          constantValue -> {
            symbol.constantValue = Optional.of(constantValue);
            availableValues.put(symbol.qualifiedName, symbol);
          });
    }

    private void validateTypedef(
        IdlTypedef typedef, Scope scope, Map<String, MutableSymbol> availableValues) {
      ResolvedType resolvedType = resolveType(typedef.type(), scope, false, availableValues);
      for (IdlDeclarator declarator : typedef.declarators()) {
        validateDeclaratorDimensions(declarator, scope, availableValues);
      }
      if (resolvedType == null) {
        return;
      }
      for (MutableSymbol symbol : typedefSymbols.getOrDefault(typedef, List.of())) {
        symbol.resolvedTypeName = Optional.of(resolvedType.name());
      }
    }

    private void validateUnion(
        IdlUnion union, Scope unionScope, Map<String, MutableSymbol> availableValues) {
      if (unionScope == null) {
        return;
      }
      Scope typeLookupScope = unionScope.parent;
      ResolvedType discriminatorType =
          resolveType(union.discriminatorType(), typeLookupScope, false, availableValues);
      if (discriminatorType == null || !validUnionDiscriminator(discriminatorType)) {
        emit(
            IdlSemanticDiagnosticCodes.INVALID_UNION_LABEL,
            "Union discriminator must be integer, char, boolean, enum, or typedef thereof",
            union.discriminatorType().span());
        return;
      }
      boolean defaultSeen = false;
      List<String> labels = new ArrayList<>();
      for (IdlUnionCase unionCase : union.cases()) {
        for (IdlUnionLabel label : unionCase.labels()) {
          if (label.defaultLabel()) {
            if (defaultSeen) {
              emit(
                  IdlSemanticDiagnosticCodes.INVALID_UNION_LABEL,
                  "Union may contain only one default label",
                  label.span());
            }
            defaultSeen = true;
          } else {
            String labelKey =
                validateUnionCaseLabel(label, discriminatorType, typeLookupScope, availableValues);
            if (!labelKey.isBlank() && labels.contains(labelKey)) {
              emit(
                  IdlSemanticDiagnosticCodes.INVALID_UNION_LABEL,
                  "Duplicate union case label: " + labelKey,
                  label.span());
            }
            labels.add(labelKey);
          }
        }
        ResolvedType memberType =
            resolveType(unionCase.type(), typeLookupScope, false, availableValues);
        validateDeclaratorDimensions(unionCase.declarator(), typeLookupScope, availableValues);
        MutableSymbol symbol = unionCaseSymbols.get(unionCase);
        if (symbol != null && memberType != null) {
          symbol.resolvedTypeName = Optional.of(memberType.name());
        }
      }
    }

    private String validateUnionCaseLabel(
        IdlUnionLabel label,
        ResolvedType discriminatorType,
        Scope scope,
        Map<String, MutableSymbol> availableValues) {
      IdlConstantExpression expression = label.expression().orElseThrow();
      Optional<IdlConstantValue> value =
          evaluateExpression(expression, discriminatorType, scope, availableValues);
      if (value.isEmpty()) {
        emit(
            IdlSemanticDiagnosticCodes.INVALID_UNION_LABEL,
            "Union case label is not valid for discriminator type: " + discriminatorType.name(),
            label.span());
        return "";
      }
      return value.orElseThrow().toString();
    }

    private void validateInterfaceInheritance(IdlInterface idlInterface, Scope scope) {
      MutableSymbol interfaceSymbol = interfaceSymbols.get(idlInterface);
      if (interfaceSymbol == null) {
        return;
      }
      List<MutableSymbol> bases = new ArrayList<>();
      for (String baseName : idlInterface.baseInterfaces()) {
        Optional<MutableSymbol> resolved =
            resolveName(
                scope, baseName, idlInterface.span(), IdlSemanticDiagnosticCodes.UNRESOLVED_NAME);
        if (resolved.isEmpty()) {
          continue;
        }
        MutableSymbol base = resolved.orElseThrow();
        if (base.kind != IdlSymbolKind.INTERFACE) {
          emit(
              IdlSemanticDiagnosticCodes.INVALID_INHERITANCE,
              "Interface base must resolve to an interface: " + baseName,
              idlInterface.span());
          continue;
        }
        if (base == interfaceSymbol) {
          emit(
              IdlSemanticDiagnosticCodes.INVALID_INHERITANCE,
              "Interface cannot inherit from itself: " + baseName,
              idlInterface.span());
          continue;
        }
        bases.add(base);
      }
      interfaceBaseGraph.put(interfaceSymbol, bases);
      if (!interfaceBaseValidationOrder.contains(interfaceSymbol)) {
        interfaceBaseValidationOrder.add(interfaceSymbol);
      }
    }

    private void validateInterfaceCycles() {
      for (MutableSymbol symbol : interfaceBaseValidationOrder) {
        detectInterfaceCycle(symbol, symbol, new ArrayList<>());
      }
    }

    private boolean detectInterfaceCycle(
        MutableSymbol root, MutableSymbol current, List<MutableSymbol> path) {
      if (path.contains(current)) {
        return false;
      }
      path.add(current);
      for (MutableSymbol base : interfaceBaseGraph.getOrDefault(current, List.of())) {
        if (base == root) {
          emit(
              IdlSemanticDiagnosticCodes.INVALID_INHERITANCE,
              "Interface inheritance cycle includes: " + root.qualifiedName,
              root.span);
          return true;
        }
        if (detectInterfaceCycle(root, base, path)) {
          return true;
        }
      }
      path.removeLast();
      return false;
    }

    private void validateDeclaratorDimensions(
        IdlDeclarator declarator, Scope scope, Map<String, MutableSymbol> availableValues) {
      for (var dimension : declarator.dimensions()) {
        validatePositiveIntegerExpression(
            dimension.size(), scope, availableValues, dimension.span());
      }
    }

    private void validateTypeBound(
        IdlTypeReference type, Scope scope, Map<String, MutableSymbol> availableValues) {
      type.bound()
          .ifPresent(
              bound ->
                  validatePositiveIntegerExpression(bound, scope, availableValues, bound.span()));
      type.elementType().ifPresent(element -> validateTypeBound(element, scope, availableValues));
    }

    private void validatePositiveIntegerExpression(
        IdlConstantExpression expression,
        Scope scope,
        Map<String, MutableSymbol> availableValues,
        SourceSpan span) {
      ResolvedType unsignedLong =
          new ResolvedType("unsigned long", ResolvedTypeKind.BUILTIN, Optional.empty());
      Optional<IdlConstantValue> value =
          evaluateExpression(expression, unsignedLong, scope, availableValues);
      if (value.isEmpty()) {
        return;
      }
      if (value.orElseThrow() instanceof IdlConstantValue.IntegerValue integer
          && integer.value().signum() > 0) {
        return;
      }
      emit(
          IdlSemanticDiagnosticCodes.INVALID_CONSTANT_VALUE,
          "IDL bounds and array dimensions must be positive integer constants",
          span);
    }

    private void validateInterfaceMember(
        IdlInterfaceMember member, Scope scope, Map<String, MutableSymbol> availableValues) {
      if (member instanceof IdlAttribute attribute) {
        ResolvedType resolvedType =
            resolveType(attribute.type(), scope.parent, false, availableValues);
        if (resolvedType != null) {
          for (MutableSymbol symbol : attributeSymbols.getOrDefault(attribute, List.of())) {
            symbol.resolvedTypeName = Optional.of(resolvedType.name());
          }
        }
        for (IdlDeclarator declarator : attribute.declarators()) {
          validateDeclaratorDimensions(declarator, scope.parent, availableValues);
        }
      } else if (member instanceof IdlOperation operation) {
        validateOperation(operation, scope, availableValues);
      }
    }

    private void validateOperation(
        IdlOperation operation, Scope interfaceScope, Map<String, MutableSymbol> availableValues) {
      Scope operationScope = operationScopes.get(operation);
      MutableSymbol operationSymbol = operationSymbols.get(operation);
      Scope typeLookupScope = interfaceScope.parent;
      ResolvedType returnType =
          resolveType(operation.returnType(), typeLookupScope, true, availableValues);
      if (operationSymbol != null && returnType != null) {
        operationSymbol.resolvedTypeName = Optional.of(returnType.name());
      }
      if (operationScope != null) {
        for (IdlParameter parameter : operation.parameters()) {
          ResolvedType parameterType =
              resolveType(parameter.type(), typeLookupScope, false, availableValues);
          MutableSymbol parameterSymbol = parameterSymbols.get(parameter);
          if (parameterSymbol != null && parameterType != null) {
            parameterSymbol.resolvedTypeName = Optional.of(parameterType.name());
          }
        }
      }
      for (String raisesName : operation.raises()) {
        validateRaisesTarget(raisesName, operation.span(), operationSymbol, typeLookupScope);
      }
    }

    private void validateRaisesTarget(
        String raisesName, SourceSpan span, MutableSymbol operationSymbol, Scope scope) {
      Optional<MutableSymbol> resolved =
          resolveName(scope, raisesName, span, IdlSemanticDiagnosticCodes.UNRESOLVED_NAME);
      if (resolved.isEmpty()) {
        return;
      }
      MutableSymbol symbol = resolved.orElseThrow();
      if (symbol.kind != IdlSymbolKind.EXCEPTION
          || operationSymbol == null
          || symbol.order >= operationSymbol.order) {
        emit(
            IdlSemanticDiagnosticCodes.INVALID_RAISES_TARGET,
            "Raises target must be a previously declared exception: " + raisesName,
            span);
      }
    }

    private Optional<IdlConstantValue> evaluateConstant(
        IdlConstant constant,
        ResolvedType resolvedType,
        Scope scope,
        Map<String, MutableSymbol> availableValues) {
      ConstantCategory category = constantCategory(resolvedType);
      if (category == ConstantCategory.INVALID) {
        emit(
            IdlSemanticDiagnosticCodes.INVALID_CONSTANT_VALUE,
            "IDL constants are not supported for type: " + resolvedType.name(),
            constant.type().span());
        return Optional.empty();
      }
      ExpressionEvaluator evaluator =
          new ExpressionEvaluator(
              constant.expression().lexemes(),
              constant.expression().span(),
              scope,
              availableValues);
      return switch (category) {
        case INTEGER -> evaluator.evaluateInteger(resolvedType.name());
        case FLOATING -> evaluator.evaluateFloating(resolvedType.name());
        case BOOLEAN -> evaluator.evaluateBoolean(resolvedType.name());
        case CHARACTER -> evaluator.evaluateCharacter(resolvedType.name());
        case STRING -> evaluator.evaluateString(resolvedType.name());
        case ENUM -> evaluator.evaluateEnum(resolvedType.name());
        case INVALID -> Optional.empty();
      };
    }

    private Optional<IdlConstantValue> evaluateExpression(
        IdlConstantExpression expression,
        ResolvedType resolvedType,
        Scope scope,
        Map<String, MutableSymbol> availableValues) {
      ConstantCategory category = constantCategory(resolvedType);
      if (category == ConstantCategory.INVALID) {
        return Optional.empty();
      }
      ExpressionEvaluator evaluator =
          new ExpressionEvaluator(expression.lexemes(), expression.span(), scope, availableValues);
      return switch (category) {
        case INTEGER -> evaluator.evaluateInteger(resolvedType.name());
        case FLOATING -> evaluator.evaluateFloating(resolvedType.name());
        case BOOLEAN -> evaluator.evaluateBoolean(resolvedType.name());
        case CHARACTER -> evaluator.evaluateCharacter(resolvedType.name());
        case STRING -> evaluator.evaluateString(resolvedType.name());
        case ENUM -> evaluator.evaluateEnum(resolvedType.name());
        case INVALID -> Optional.empty();
      };
    }

    private ConstantCategory constantCategory(ResolvedType resolvedType) {
      if (resolvedType.kind() == ResolvedTypeKind.ENUM) {
        return ConstantCategory.ENUM;
      }
      if (resolvedType.kind() != ResolvedTypeKind.BUILTIN
          && resolvedType.kind() != ResolvedTypeKind.TYPEDEF) {
        return ConstantCategory.INVALID;
      }
      String name = resolvedType.name();
      if (INTEGER_CONSTANT_TYPES.contains(name)) {
        return ConstantCategory.INTEGER;
      }
      if (FLOATING_CONSTANT_TYPES.contains(name)) {
        return ConstantCategory.FLOATING;
      }
      if (name.equals("boolean")) {
        return ConstantCategory.BOOLEAN;
      }
      if (name.equals("char") || name.equals("wchar")) {
        return ConstantCategory.CHARACTER;
      }
      if (name.equals("string") || name.equals("wstring")) {
        return ConstantCategory.STRING;
      }
      return ConstantCategory.INVALID;
    }

    private boolean validUnionDiscriminator(ResolvedType resolvedType) {
      ConstantCategory category = constantCategory(resolvedType);
      return category == ConstantCategory.INTEGER
          || category == ConstantCategory.CHARACTER
          || category == ConstantCategory.BOOLEAN
          || category == ConstantCategory.ENUM;
    }

    private ResolvedType resolveType(
        IdlTypeReference type,
        Scope scope,
        boolean allowVoid,
        Map<String, MutableSymbol> availableValues) {
      if (type.kind() == IdlTypeReferenceKind.SEQUENCE) {
        ResolvedType elementType =
            resolveType(type.elementType().orElseThrow(), scope, false, availableValues);
        validateTypeBound(type, scope, availableValues);
        return elementType == null
            ? null
            : new ResolvedType(type.name(), ResolvedTypeKind.SEQUENCE, Optional.empty());
      }
      if (type.kind() == IdlTypeReferenceKind.BOUNDED_STRING) {
        validateTypeBound(type, scope, availableValues);
        String name = type.name().startsWith("wstring<") ? "wstring" : "string";
        return new ResolvedType(name, ResolvedTypeKind.BUILTIN, Optional.empty());
      }
      if (type.name().equals("void")) {
        if (allowVoid) {
          return new ResolvedType("void", ResolvedTypeKind.BUILTIN, Optional.empty());
        }
        emit(
            IdlSemanticDiagnosticCodes.INVALID_TYPE_REFERENCE,
            "void is only valid as an operation return type",
            type.span());
        return null;
      }
      if (BUILTIN_TYPES.contains(type.name())) {
        return new ResolvedType(type.name(), ResolvedTypeKind.BUILTIN, Optional.empty());
      }
      Optional<MutableSymbol> resolved =
          resolveName(scope, type.name(), type.span(), IdlSemanticDiagnosticCodes.UNRESOLVED_NAME);
      if (resolved.isEmpty()) {
        return null;
      }
      MutableSymbol symbol = resolved.orElseThrow();
      ResolvedTypeKind kind = typeKind(symbol.kind);
      if (kind == ResolvedTypeKind.INVALID) {
        emit(
            IdlSemanticDiagnosticCodes.INVALID_TYPE_REFERENCE,
            "Name is not valid as a type reference: " + type.name(),
            type.span());
        return null;
      }
      String resolvedName =
          symbol.kind == IdlSymbolKind.TYPEDEF
              ? symbol.resolvedTypeName.orElse(symbol.qualifiedName)
              : symbol.qualifiedName;
      return new ResolvedType(resolvedName, kind, Optional.of(symbol));
    }

    private Optional<MutableSymbol> resolveName(
        Scope scope, String name, SourceSpan span, DiagnosticCode unresolvedCode) {
      NameParts nameParts = NameParts.parse(name);
      if (nameParts.segments().isEmpty()) {
        emit(unresolvedCode, "Unresolved IDL name: " + name, span);
        return Optional.empty();
      }
      ResolveResult result =
          nameParts.absolute()
              ? resolveFromScope(globalScope, nameParts.segments())
              : resolveRelative(scope, nameParts.segments());
      if (result.caseMismatch()) {
        emit(
            IdlSemanticDiagnosticCodes.CASE_MISMATCH,
            "IDL reference differs from the defining identifier only by case: " + name,
            span);
        return Optional.empty();
      }
      if (result.symbol().isEmpty()) {
        emit(unresolvedCode, "Unresolved IDL name: " + name, span);
      }
      return result.symbol();
    }

    private ResolveResult resolveRelative(Scope scope, List<String> segments) {
      Scope current = scope;
      while (current != null) {
        ResolveResult result = resolveFromScope(current, segments);
        if (result.symbol().isPresent() || result.caseMismatch() || result.shadowed()) {
          return result;
        }
        current = current.parent;
      }
      return ResolveResult.unresolved(false);
    }

    private ResolveResult resolveFromScope(Scope scope, List<String> segments) {
      Scope currentScope = scope;
      MutableSymbol currentSymbol = null;
      for (int index = 0; index < segments.size(); index++) {
        String segment = segments.get(index);
        MutableSymbol candidate = currentScope.symbolsByLowerName.get(key(segment));
        if (candidate == null) {
          return currentSymbol == null
              ? ResolveResult.unresolved(false)
              : ResolveResult.unresolved(true);
        }
        if (!candidate.name.equals(segment)) {
          return ResolveResult.mismatchedCase();
        }
        currentSymbol = candidate;
        currentScope = candidate.childScope;
        if (currentScope == null && index < segments.size() - 1) {
          return ResolveResult.unresolved(true);
        }
      }
      return ResolveResult.resolved(currentSymbol);
    }

    private static ResolvedTypeKind typeKind(IdlSymbolKind symbolKind) {
      return switch (symbolKind) {
        case ENUM -> ResolvedTypeKind.ENUM;
        case EXCEPTION -> ResolvedTypeKind.EXCEPTION;
        case INTERFACE -> ResolvedTypeKind.INTERFACE;
        case NATIVE -> ResolvedTypeKind.NATIVE;
        case STRUCT -> ResolvedTypeKind.STRUCT;
        case TYPEDEF -> ResolvedTypeKind.TYPEDEF;
        case UNION -> ResolvedTypeKind.UNION;
        case VALUE_BOX -> ResolvedTypeKind.VALUE_BOX;
        case VALUETYPE -> ResolvedTypeKind.VALUETYPE;
        default -> ResolvedTypeKind.INVALID;
      };
    }

    private MutableSymbol resolveDefinedSymbol(Scope scope) {
      if (scope == null || scope.parent == null) {
        return null;
      }
      String simpleName = scope.qualifiedName.substring(scope.qualifiedName.lastIndexOf("::") + 2);
      return scope.parent.symbolsByLowerName.get(key(simpleName));
    }

    private List<IdlSymbol> freezeSymbols() {
      return symbols.stream().map(MutableSymbol::freeze).toList();
    }

    private void emit(DiagnosticCode code, String message, SourceSpan span) {
      diagnostics.add(Diagnostic.withSpan(code, DiagnosticSeverity.ERROR, message, span));
    }

    private boolean hasErrors() {
      return diagnostics.stream()
          .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
    }

    private static String key(String name) {
      return name.toLowerCase(Locale.ROOT);
    }

    private final class ExpressionEvaluator {

      private final List<String> lexemes;
      private final SourceSpan span;
      private final Scope scope;
      private final Map<String, MutableSymbol> availableValues;
      private int current;

      private ExpressionEvaluator(
          List<String> lexemes,
          SourceSpan span,
          Scope scope,
          Map<String, MutableSymbol> availableValues) {
        this.lexemes = List.copyOf(lexemes);
        this.span = span;
        this.scope = scope;
        this.availableValues = availableValues;
      }

      private Optional<IdlConstantValue> evaluateInteger(String idlType) {
        Optional<BigInteger> value = parseBitwiseOr();
        if (value.isEmpty() || !consumeEnd()) {
          return Optional.empty();
        }
        return Optional.of(IdlConstantValue.integer(idlType, value.orElseThrow()));
      }

      private Optional<IdlConstantValue> evaluateFloating(String idlType) {
        Optional<IdlConstantValue> reference = singleReferenceValue();
        if (reference.isPresent()) {
          IdlConstantValue value = reference.orElseThrow();
          if (value instanceof IdlConstantValue.FloatingValue floating) {
            return Optional.of(IdlConstantValue.floating(idlType, floating.value()));
          }
          if (value instanceof IdlConstantValue.IntegerValue integer) {
            return Optional.of(IdlConstantValue.floating(idlType, new BigDecimal(integer.value())));
          }
          emitInvalidConstantValue("Constant reference is not numeric");
          return Optional.empty();
        }
        int sign = 1;
        if (match("+")) {
          sign = 1;
        } else if (match("-")) {
          sign = -1;
        }
        if (isAtEnd() || !isNumericLiteral(peek())) {
          emitInvalidExpression("Expected floating constant literal");
          return Optional.empty();
        }
        String literal = advance();
        if (!consumeEnd()) {
          return Optional.empty();
        }
        BigDecimal decimal = parseDecimalLiteral(literal);
        if (sign < 0) {
          decimal = decimal.negate();
        }
        return Optional.of(IdlConstantValue.floating(idlType, decimal));
      }

      private Optional<IdlConstantValue> evaluateBoolean(String idlType) {
        if (lexemes.size() == 1
            && (lexemes.getFirst().equals("TRUE") || lexemes.getFirst().equals("FALSE"))) {
          return Optional.of(IdlConstantValue.bool(idlType, lexemes.getFirst().equals("TRUE")));
        }
        Optional<IdlConstantValue> reference = singleReferenceValue();
        if (reference.isPresent()) {
          IdlConstantValue value = reference.orElseThrow();
          if (value instanceof IdlConstantValue.BooleanValue bool) {
            return Optional.of(IdlConstantValue.bool(idlType, bool.value()));
          }
          emitInvalidConstantValue("Constant reference is not boolean");
          return Optional.empty();
        }
        emitInvalidExpression("Expected boolean constant literal");
        return Optional.empty();
      }

      private Optional<IdlConstantValue> evaluateCharacter(String idlType) {
        Optional<IdlConstantValue> reference = singleReferenceValue();
        if (reference.isPresent()) {
          IdlConstantValue value = reference.orElseThrow();
          if (value instanceof IdlConstantValue.CharacterValue character) {
            return Optional.of(IdlConstantValue.character(idlType, character.value()));
          }
          emitInvalidConstantValue("Constant reference is not a character");
          return Optional.empty();
        }
        if (lexemes.size() != 1 || !isCharacterLiteral(lexemes.getFirst())) {
          emitInvalidExpression("Expected character constant literal");
          return Optional.empty();
        }
        String value = decodeQuotedLiteral(lexemes.getFirst());
        if (value.codePointCount(0, value.length()) != 1) {
          emitInvalidConstantValue("Character constants must contain exactly one character");
          return Optional.empty();
        }
        return Optional.of(IdlConstantValue.character(idlType, value));
      }

      private Optional<IdlConstantValue> evaluateString(String idlType) {
        Optional<IdlConstantValue> reference = singleReferenceValue();
        if (reference.isPresent()) {
          IdlConstantValue value = reference.orElseThrow();
          if (value instanceof IdlConstantValue.StringValue string) {
            return Optional.of(IdlConstantValue.string(idlType, string.value()));
          }
          emitInvalidConstantValue("Constant reference is not a string");
          return Optional.empty();
        }
        if (lexemes.size() != 1 || !isStringLiteral(lexemes.getFirst())) {
          emitInvalidExpression("Expected string constant literal");
          return Optional.empty();
        }
        return Optional.of(
            IdlConstantValue.string(idlType, decodeQuotedLiteral(lexemes.getFirst())));
      }

      private Optional<IdlConstantValue> evaluateEnum(String idlType) {
        Optional<IdlConstantValue> reference = singleReferenceValue();
        if (reference.isEmpty()) {
          emitInvalidExpression("Expected enum constant reference");
          return Optional.empty();
        }
        IdlConstantValue value = reference.orElseThrow();
        if (value instanceof IdlConstantValue.EnumeratorValue enumerator
            && enumerator.idlType().equals(idlType)) {
          return Optional.of(enumerator);
        }
        emitInvalidConstantValue("Enum constant reference does not match declared enum type");
        return Optional.empty();
      }

      private Optional<BigInteger> parseBitwiseOr() {
        Optional<BigInteger> value = parseBitwiseXor();
        while (value.isPresent() && match("|")) {
          Optional<BigInteger> right = parseBitwiseXor();
          value = right.map(value.orElseThrow()::or);
        }
        return value;
      }

      private Optional<BigInteger> parseBitwiseXor() {
        Optional<BigInteger> value = parseBitwiseAnd();
        while (value.isPresent() && match("^")) {
          Optional<BigInteger> right = parseBitwiseAnd();
          value = right.map(value.orElseThrow()::xor);
        }
        return value;
      }

      private Optional<BigInteger> parseBitwiseAnd() {
        Optional<BigInteger> value = parseShift();
        while (value.isPresent() && match("&")) {
          Optional<BigInteger> right = parseShift();
          value = right.map(value.orElseThrow()::and);
        }
        return value;
      }

      private Optional<BigInteger> parseShift() {
        Optional<BigInteger> value = parseAdditive();
        while (value.isPresent() && (check("<<") || check(">>"))) {
          String operator = advance();
          Optional<BigInteger> right = parseAdditive();
          if (right.isEmpty()) {
            return Optional.empty();
          }
          BigInteger shift = right.orElseThrow();
          if (shift.signum() < 0 || shift.bitLength() > 31) {
            emitInvalidConstantValue("Shift count is outside the supported integer range");
            return Optional.empty();
          }
          value =
              Optional.of(
                  operator.equals("<<")
                      ? value.orElseThrow().shiftLeft(shift.intValue())
                      : value.orElseThrow().shiftRight(shift.intValue()));
        }
        return value;
      }

      private Optional<BigInteger> parseAdditive() {
        Optional<BigInteger> value = parseMultiplicative();
        while (value.isPresent() && (check("+") || check("-"))) {
          String operator = advance();
          Optional<BigInteger> right = parseMultiplicative();
          if (right.isEmpty()) {
            return Optional.empty();
          }
          BigInteger left = value.orElseThrow();
          BigInteger number = right.orElseThrow();
          value = Optional.of(operator.equals("+") ? left.add(number) : left.subtract(number));
        }
        return value;
      }

      private Optional<BigInteger> parseMultiplicative() {
        Optional<BigInteger> value = parseUnary();
        while (value.isPresent() && (check("*") || check("/") || check("%"))) {
          String operator = advance();
          Optional<BigInteger> right = parseUnary();
          if (right.isEmpty()) {
            return Optional.empty();
          }
          BigInteger divisor = right.orElseThrow();
          if ((operator.equals("/") || operator.equals("%")) && divisor.signum() == 0) {
            emitInvalidConstantValue("Integer constant expression divides by zero");
            return Optional.empty();
          }
          value =
              Optional.of(
                  switch (operator) {
                    case "*" -> value.orElseThrow().multiply(divisor);
                    case "/" -> value.orElseThrow().divide(divisor);
                    case "%" -> value.orElseThrow().remainder(divisor);
                    default -> throw new IllegalStateException("Unexpected operator: " + operator);
                  });
        }
        return value;
      }

      private Optional<BigInteger> parseUnary() {
        if (match("+")) {
          return parseUnary();
        }
        if (match("-")) {
          return parseUnary().map(BigInteger::negate);
        }
        if (match("~")) {
          return parseUnary().map(BigInteger::not);
        }
        return parsePrimary();
      }

      private Optional<BigInteger> parsePrimary() {
        if (match("(")) {
          Optional<BigInteger> value = parseBitwiseOr();
          if (!match(")")) {
            emitInvalidExpression("Expected ')' in constant expression");
            return Optional.empty();
          }
          return value;
        }
        if (isAtEnd()) {
          emitInvalidExpression("Unexpected end of constant expression");
          return Optional.empty();
        }
        if (isIntegerLiteral(peek())) {
          return Optional.of(parseIntegerLiteral(advance()));
        }
        Optional<IdlConstantValue> reference = parseReferenceValue();
        if (reference.isEmpty()) {
          return Optional.empty();
        }
        IdlConstantValue value = reference.orElseThrow();
        if (value instanceof IdlConstantValue.IntegerValue integer) {
          return Optional.of(integer.value());
        }
        emitInvalidConstantValue("Constant reference is not integer");
        return Optional.empty();
      }

      private Optional<IdlConstantValue> singleReferenceValue() {
        if (lexemes.isEmpty() || !startsName(lexemes.getFirst())) {
          return Optional.empty();
        }
        int original = current;
        Optional<IdlConstantValue> value = parseReferenceValue();
        if (value.isPresent() && isAtEnd()) {
          return value;
        }
        current = original;
        return Optional.empty();
      }

      private Optional<IdlConstantValue> parseReferenceValue() {
        Optional<String> name = parseScopedName();
        if (name.isEmpty()) {
          emitInvalidExpression("Expected constant reference");
          return Optional.empty();
        }
        Optional<MutableSymbol> resolved =
            resolveName(
                scope, name.orElseThrow(), span, IdlSemanticDiagnosticCodes.UNRESOLVED_NAME);
        if (resolved.isEmpty()) {
          return Optional.empty();
        }
        MutableSymbol symbol = resolved.orElseThrow();
        MutableSymbol available = availableValues.get(symbol.qualifiedName);
        if (available == null) {
          emitInvalidExpression(
              "Constant reference is not declared before use: " + name.orElseThrow());
          return Optional.empty();
        }
        if (available.constantValue.isEmpty()) {
          emitInvalidConstantValue(
              "Resolved constant reference has no value: " + name.orElseThrow());
          return Optional.empty();
        }
        return available.constantValue;
      }

      private Optional<String> parseScopedName() {
        if (isAtEnd()) {
          return Optional.empty();
        }
        StringBuilder name = new StringBuilder();
        if (match("::")) {
          name.append("::");
        }
        if (isAtEnd() || !isIdentifier(peek())) {
          return Optional.empty();
        }
        name.append(normalizeIdentifier(advance()));
        while (match("::")) {
          if (isAtEnd() || !isIdentifier(peek())) {
            emitInvalidExpression("Expected identifier after '::'");
            return Optional.empty();
          }
          name.append("::").append(normalizeIdentifier(advance()));
        }
        return Optional.of(name.toString());
      }

      private boolean consumeEnd() {
        if (isAtEnd()) {
          return true;
        }
        emitInvalidExpression("Unexpected token in constant expression: " + peek());
        return false;
      }

      private boolean match(String lexeme) {
        if (!check(lexeme)) {
          return false;
        }
        current++;
        return true;
      }

      private boolean check(String lexeme) {
        return !isAtEnd() && peek().equals(lexeme);
      }

      private String advance() {
        return lexemes.get(current++);
      }

      private String peek() {
        return lexemes.get(current);
      }

      private boolean isAtEnd() {
        return current >= lexemes.size();
      }

      private void emitInvalidExpression(String message) {
        emit(IdlSemanticDiagnosticCodes.INVALID_CONSTANT_EXPRESSION, message, span);
      }

      private void emitInvalidConstantValue(String message) {
        emit(IdlSemanticDiagnosticCodes.INVALID_CONSTANT_VALUE, message, span);
      }
    }

    private static boolean startsName(String lexeme) {
      return lexeme.equals("::") || isIdentifier(lexeme);
    }

    private static boolean isIdentifier(String lexeme) {
      return IDENTIFIER_PATTERN.matcher(lexeme).matches();
    }

    private static String normalizeIdentifier(String lexeme) {
      return lexeme.startsWith("_") ? lexeme.substring(1) : lexeme;
    }

    private static boolean isIntegerLiteral(String lexeme) {
      return lexeme.matches("0[xX][0-9A-Fa-f]+")
          || lexeme.matches("0[0-7]*")
          || lexeme.matches("[1-9][0-9]*");
    }

    private static boolean isNumericLiteral(String lexeme) {
      return isIntegerLiteral(lexeme)
          || lexeme.matches("([0-9]+\\.[0-9]*|\\.[0-9]+|[0-9]+)[eE][+-]?[0-9]+")
          || lexeme.matches("[0-9]+\\.[0-9]*")
          || lexeme.matches("\\.[0-9]+");
    }

    private static BigInteger parseIntegerLiteral(String lexeme) {
      if (lexeme.startsWith("0x") || lexeme.startsWith("0X")) {
        return new BigInteger(lexeme.substring(2), 16);
      }
      if (lexeme.length() > 1 && lexeme.startsWith("0")) {
        return new BigInteger(lexeme.substring(1), 8);
      }
      return new BigInteger(lexeme);
    }

    private static BigDecimal parseDecimalLiteral(String lexeme) {
      if (isIntegerLiteral(lexeme)) {
        return new BigDecimal(parseIntegerLiteral(lexeme));
      }
      return new BigDecimal(lexeme);
    }

    private static boolean isCharacterLiteral(String lexeme) {
      return lexeme.startsWith("'") || lexeme.startsWith("L'");
    }

    private static boolean isStringLiteral(String lexeme) {
      return lexeme.startsWith("\"") || lexeme.startsWith("L\"");
    }

    private static String decodeQuotedLiteral(String lexeme) {
      int start = lexeme.startsWith("L") ? 2 : 1;
      int end = lexeme.length() - 1;
      StringBuilder decoded = new StringBuilder();
      int index = start;
      while (index < end) {
        char current = lexeme.charAt(index);
        if (current != '\\') {
          decoded.append(current);
          index++;
          continue;
        }
        index++;
        char escaped = lexeme.charAt(index);
        switch (escaped) {
          case 'n' -> decoded.append('\n');
          case 't' -> decoded.append('\t');
          case 'b' -> decoded.append('\b');
          case 'r' -> decoded.append('\r');
          case 'f' -> decoded.append('\f');
          case '\\' -> decoded.append('\\');
          case '\'' -> decoded.append('\'');
          case '"' -> decoded.append('"');
          case 'x' -> {
            int hexEnd = Math.min(index + 3, end);
            decoded.append((char) Integer.parseInt(lexeme.substring(index + 1, hexEnd), 16));
            index = hexEnd - 1;
          }
          case 'u' -> {
            int unicodeEnd = Math.min(index + 5, end);
            decoded.append((char) Integer.parseInt(lexeme.substring(index + 1, unicodeEnd), 16));
            index = unicodeEnd - 1;
          }
          default -> {
            if (escaped >= '0' && escaped <= '7') {
              int octalEnd = index + 1;
              while (octalEnd < end
                  && octalEnd < index + 3
                  && lexeme.charAt(octalEnd) >= '0'
                  && lexeme.charAt(octalEnd) <= '7') {
                octalEnd++;
              }
              decoded.append((char) Integer.parseInt(lexeme.substring(index, octalEnd), 8));
              index = octalEnd - 1;
            } else {
              decoded.append(escaped);
            }
          }
        }
        index++;
      }
      return decoded.toString();
    }
  }

  private enum ConstantCategory {
    INTEGER,
    FLOATING,
    BOOLEAN,
    CHARACTER,
    STRING,
    ENUM,
    INVALID
  }

  private enum ResolvedTypeKind {
    BUILTIN,
    ENUM,
    EXCEPTION,
    INTERFACE,
    NATIVE,
    SEQUENCE,
    STRUCT,
    TYPEDEF,
    UNION,
    VALUE_BOX,
    VALUETYPE,
    INVALID
  }

  private record ResolvedType(String name, ResolvedTypeKind kind, Optional<MutableSymbol> symbol) {}

  private record ResolveResult(
      Optional<MutableSymbol> symbol, boolean caseMismatch, boolean shadowed) {

    private static ResolveResult resolved(MutableSymbol symbol) {
      return new ResolveResult(Optional.of(symbol), false, false);
    }

    private static ResolveResult unresolved(boolean shadowed) {
      return new ResolveResult(Optional.empty(), false, shadowed);
    }

    private static ResolveResult mismatchedCase() {
      return new ResolveResult(Optional.empty(), true, true);
    }
  }

  private record NameParts(boolean absolute, List<String> segments) {

    private static NameParts parse(String name) {
      boolean absolute = name.startsWith("::");
      String withoutPrefix = absolute ? name.substring(2) : name;
      if (withoutPrefix.isBlank()) {
        return new NameParts(absolute, List.of());
      }
      return new NameParts(absolute, List.of(withoutPrefix.split("::")));
    }
  }

  private static final class Scope {

    private final Scope parent;
    private final String qualifiedName;
    private final Map<String, MutableSymbol> symbolsByLowerName = new HashMap<>();

    private Scope(Scope parent, String qualifiedName) {
      this.parent = parent;
      this.qualifiedName = qualifiedName;
    }

    private String childQualifiedName(String name) {
      return qualifiedName.isEmpty() ? "::" + name : qualifiedName + "::" + name;
    }
  }

  private static final class MutableSymbol {

    private final IdlSymbolKind kind;
    private final String name;
    private final String qualifiedName;
    private final Optional<String> typeName;
    private final SourceSpan span;
    private final int order;
    private Optional<String> resolvedTypeName = Optional.empty();
    private Optional<IdlConstantValue> constantValue = Optional.empty();
    private Scope childScope;

    private MutableSymbol(
        IdlSymbolKind kind,
        String name,
        String qualifiedName,
        Optional<String> typeName,
        SourceSpan span,
        int order) {
      this.kind = kind;
      this.name = name;
      this.qualifiedName = qualifiedName;
      this.typeName = typeName;
      this.span = span;
      this.order = order;
    }

    private IdlSymbol freeze() {
      return new IdlSymbol(
          kind, name, qualifiedName, typeName, resolvedTypeName, constantValue, span);
    }
  }
}
