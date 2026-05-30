package io.github.mundanej.mjo.idl.java.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.idl.parser.IdlParseResult;
import io.github.mundanej.mjo.idl.parser.IdlParser;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticAnalyzer;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticModel;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link IdlJavaMapper}. */
@Tag("unit")
final class IdlJavaMapperTest {

  private final IdlParser parser = new IdlParser();
  private final IdlSemanticAnalyzer analyzer = new IdlSemanticAnalyzer();
  private final IdlJavaMapper mapper = new IdlJavaMapper();

  @Test
  void mapsMinimalSemanticModelDeterministicallyInBothModes() {
    IdlSemanticModel semanticModel =
        semanticModel(
            """
            module Demo {
              const long BASE = 7;
              enum Color { RED, GREEN };
              const Color FAVORITE = Color::GREEN;
              struct Point { long x; string label; };
              exception Bad { string reason; };
              interface Shape {
                readonly attribute string name;
                attribute long size;
                Point move(in Point value, out long count) raises (Bad);
              };
            };
            """);

    JavaMappingModel legacy = mapper.map(semanticModel, JavaMappingMode.LEGACY_COMPATIBILITY);
    JavaMappingModel modern = mapper.map(semanticModel, JavaMappingMode.MODERN);

    assertEquals(legacy, mapper.map(semanticModel, JavaMappingMode.LEGACY_COMPATIBILITY));
    assertEquals(JavaMappingMode.LEGACY_COMPATIBILITY, legacy.mode());
    assertEquals("mapping-test.idl", legacy.sourceName());
    assertEquals(
        List.of("demo.Color", "demo.Point", "demo.Bad", "demo.Shape"),
        legacy.types().stream()
            .filter(type -> type.kind() != JavaMappedTypeKind.HOLDER)
            .map(type -> type.name().qualifiedName())
            .toList());
    assertEquals(
        List.of("demo.IdlConstants"),
        legacy.constantScopes().stream().map(scope -> scope.name().qualifiedName()).toList());
    assertEquals(
        List.of("modern.demo.Color", "modern.demo.Point", "modern.demo.Bad", "modern.demo.Shape"),
        modern.types().stream()
            .filter(type -> type.kind() != JavaMappedTypeKind.HOLDER)
            .map(type -> type.name().qualifiedName())
            .toList());
    assertEquals("demo.LongHolder", legacy.types().getLast().name().qualifiedName());

    JavaMappedType shape = legacy.types().get(3);
    assertEquals(JavaMappedTypeKind.INTERFACE, shape.kind());
    assertEquals(
        List.of("name", "size"),
        shape.attributes().stream().map(JavaMappedAttribute::name).toList());
    assertEquals("demo.Point", shape.operations().getFirst().returnType());
    assertEquals(List.of("demo.Bad"), shape.operations().getFirst().thrownTypes());
    assertEquals(
        List.of("BASE", "FAVORITE"),
        legacy.constantScopes().getFirst().constants().stream()
            .map(JavaMappedConstant::name)
            .toList());
    assertEquals(
        "demo.Color.GREEN", legacy.constantScopes().getFirst().constants().get(1).initializer());
  }

  @Test
  void escapesJavaKeywordsAndKeepsCollectionsImmutable() {
    JavaMappingModel model =
        mapper.map(
            semanticModel(
                """
                module _class {
                  struct Holder { long _int; };
                };
                """),
            JavaMappingMode.LEGACY_COMPATIBILITY);

    assertEquals("class_", model.types().getFirst().name().packageName());
    assertEquals("Holder", model.types().getFirst().name().simpleName());
    assertEquals("int_", model.types().getFirst().fields().getFirst().name());
    assertThrows(UnsupportedOperationException.class, () -> model.types().clear());
    assertThrows(
        UnsupportedOperationException.class, () -> model.types().getFirst().fields().clear());
    assertThrows(IllegalArgumentException.class, () -> new JavaMappedName("", " "));
  }

  @Test
  void mapsPrimitiveTypesConstantsAndDefaultPackageDeclarations() {
    JavaMappingModel model =
        mapper.map(
            semanticModel(
                """
                const long TOP = 9;
                struct Global { long x; };
                interface RootApi { ::Global current(); };
                module Scalars {
                  const short SHORT_VALUE = 1;
                  const unsigned short UNSIGNED_SHORT_VALUE = 2;
                  const unsigned long UNSIGNED_LONG_VALUE = 3;
                  const long long LONG_LONG_VALUE = 4;
                  const unsigned long long UNSIGNED_LONG_LONG_VALUE = 5;
                  const float FLOAT_VALUE = 1.5;
                  const double DOUBLE_VALUE = -2.5;
                  const long double LONG_DOUBLE_VALUE = 3.5;
                  const boolean BOOLEAN_VALUE = TRUE;
                  const char CHARACTER_VALUE = '\\n';
                  const string STRING_VALUE = "a\\n";
                  struct Values {
                    boolean ok;
                    char letter;
                    octet data;
                    short s;
                    unsigned short us;
                    long l;
                    unsigned long ul;
                    long long ll;
                    unsigned long long ull;
                    float f;
                    double d;
                    long double ld;
                    string text;
                    any payload;
                  };
                };
                """),
            JavaMappingMode.LEGACY_COMPATIBILITY);

    assertEquals("", model.types().getFirst().name().packageName());
    assertEquals("Global", model.types().getFirst().name().simpleName());
    assertEquals("Global", model.types().get(1).operations().getFirst().returnType());

    JavaMappedConstantScope topLevelConstants = constantScope(model, "IdlConstants");
    JavaMappedConstantScope scalarConstants = constantScope(model, "scalars.IdlConstants");
    assertEquals("IdlConstants", topLevelConstants.name().qualifiedName());
    assertEquals(
        List.of(
            "(short) 1",
            "2",
            "3L",
            "4L",
            "new java.math.BigInteger(\"5\")",
            "1.5f",
            "-2.5d",
            "new java.math.BigDecimal(\"3.5\")",
            "true",
            "'\\n'",
            "\"a\\n\""),
        scalarConstants.constants().stream().map(JavaMappedConstant::initializer).toList());

    JavaMappedType values = model.types().get(2);
    assertEquals(
        List.of(
            "boolean",
            "char",
            "short",
            "short",
            "int",
            "int",
            "long",
            "long",
            "java.math.BigInteger",
            "float",
            "double",
            "java.math.BigDecimal",
            "java.lang.String",
            "java.lang.Object"),
        values.fields().stream().map(JavaMappedField::javaType).toList());
  }

  @Test
  void mapsNamesTypesConstantsAndValueObjectAliasesAcrossScopes() {
    JavaMappingModel model =
        mapper.map(
            semanticModel(
                """
                module Outer {
                  module Inner {
                    enum MixedCase { firstValue, second_value };
                    const MixedCase CHOSEN = MixedCase::firstValue;
                    const long MASK = (1 << 4) | 3;
                    struct ValueHolder { any value; MixedCase color; };
                    interface Service {
                      readonly attribute any result;
                      any invoke(in ValueHolder payload, in any anyValue);
                    };
                  };
                };
                """),
            JavaMappingMode.MODERN);

    assertEquals(
        List.of(
            "modern.outer.inner.MixedCase",
            "modern.outer.inner.ValueHolder",
            "modern.outer.inner.Service"),
        model.types().stream().map(type -> type.name().qualifiedName()).toList());

    JavaMappedType mixedCase = model.types().get(0);
    assertEquals(List.of("FIRST_VALUE", "SECOND_VALUE"), mixedCase.enumConstants());

    JavaMappedType valueObject = model.types().get(1);
    assertEquals(JavaMappedTypeKind.STRUCT, valueObject.kind());
    assertEquals(
        List.of("java.lang.Object", "modern.outer.inner.MixedCase"),
        valueObject.fields().stream().map(JavaMappedField::javaType).toList());
    assertEquals(
        List.of("value", "color"),
        valueObject.fields().stream().map(JavaMappedField::name).toList());

    JavaMappedType service = model.types().get(2);
    assertEquals(JavaMappedTypeKind.INTERFACE, service.kind());
    assertEquals("java.lang.Object", service.attributes().getFirst().javaType());
    assertEquals("java.lang.Object", service.operations().getFirst().returnType());
    assertEquals(
        List.of("modern.outer.inner.ValueHolder", "java.lang.Object"),
        service.operations().getFirst().parameters().stream()
            .map(JavaMappedParameter::javaType)
            .toList());

    JavaMappedConstantScope constants = constantScope(model, "modern.outer.inner.IdlConstants");
    assertEquals(
        List.of("modern.outer.inner.MixedCase.FIRST_VALUE", "19"),
        constants.constants().stream().map(JavaMappedConstant::initializer).toList());
  }

  @Test
  void mappedValueRecordsValidateInputsAndDefensivelyCopyCollections() {
    JavaMappedField field = new JavaMappedField("int", "count");
    JavaMappedType type =
        new JavaMappedType(
            JavaMappedTypeKind.STRUCT,
            new JavaMappedName("demo", "Counter"),
            new java.util.ArrayList<>(List.of(field)),
            new java.util.ArrayList<>(),
            new java.util.ArrayList<>(),
            new java.util.ArrayList<>());
    JavaMappingModel model =
        new JavaMappingModel(
            JavaMappingMode.LEGACY_COMPATIBILITY,
            "manual.idl",
            new java.util.ArrayList<>(List.of(type)),
            List.of(
                new JavaMappedConstantScope(
                    new JavaMappedName("demo", "IdlConstants"),
                    new java.util.ArrayList<>(
                        List.of(new JavaMappedConstant("int", "COUNT", "1"))))));

    assertEquals("demo.Counter", type.name().qualifiedName());
    assertThrows(UnsupportedOperationException.class, () -> type.fields().add(field));
    assertThrows(UnsupportedOperationException.class, () -> model.constantScopes().clear());
    assertThrows(IllegalArgumentException.class, () -> new JavaMappedField(" ", "count"));
    assertThrows(IllegalArgumentException.class, () -> new JavaMappedParameter("int", ""));
    assertThrows(
        IllegalArgumentException.class,
        () -> new JavaMappedOperation("void", " ", List.of(), List.of()));
    assertThrows(IllegalArgumentException.class, () -> new JavaMappedAttribute("", "value", true));
    assertThrows(
        IllegalArgumentException.class,
        () -> new JavaMappedConstantScope(new JavaMappedName("demo", "Empty"), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new JavaMappingModel(JavaMappingMode.MODERN, " ", List.of(), List.of()));
  }

  @Test
  void mapsApprovedRmiGeneratedIdlFixture() {
    JavaMappingModel model =
        mapper.map(
            semanticModel("rmi-generated.idl", rmiGeneratedIdlFixture()),
            JavaMappingMode.LEGACY_COMPATIBILITY);

    assertEquals("rmi-generated.idl", model.sourceName());
    assertEquals(
        List.of("example.calc.CalculatorProblem", "example.calc.Calculator"),
        model.types().stream().map(type -> type.name().qualifiedName()).toList());

    JavaMappedType problem = model.types().get(0);
    JavaMappedType calculator = model.types().get(1);
    assertEquals(JavaMappedTypeKind.EXCEPTION, problem.kind());
    assertEquals(JavaMappedTypeKind.INTERFACE, calculator.kind());
    assertEquals(
        List.of("add", "describe", "clear"),
        calculator.operations().stream().map(JavaMappedOperation::name).toList());
    assertEquals(
        List.of("int", "java.lang.String", "void"),
        calculator.operations().stream().map(JavaMappedOperation::returnType).toList());
    assertEquals(
        List.of("example.calc.CalculatorProblem"), calculator.operations().get(1).thrownTypes());
  }

  @Test
  void mapsG10LegacyGrammarClosureSurface() {
    JavaMappingModel model =
        mapper.map(semanticModel(g10PeerStyleIdl()), JavaMappingMode.LEGACY_COMPATIBILITY);

    assertEquals(
        List.of(
            JavaMappedTypeKind.INTERFACE_FORWARD,
            JavaMappedTypeKind.TYPEDEF,
            JavaMappedTypeKind.TYPEDEF,
            JavaMappedTypeKind.TYPEDEF,
            JavaMappedTypeKind.UNION,
            JavaMappedTypeKind.EXCEPTION,
            JavaMappedTypeKind.INTERFACE,
            JavaMappedTypeKind.INTERFACE),
        model.types().stream()
            .filter(type -> type.kind() != JavaMappedTypeKind.HOLDER)
            .map(JavaMappedType::kind)
            .toList());
    assertEquals("g10.Names", model.types().get(1).name().qualifiedName());
    assertEquals("java.lang.String[]", model.types().get(1).aliasType());
    assertEquals("int[][]", model.types().get(2).aliasType());
    assertEquals(
        List.of("java.lang.String", "java.lang.String[]"),
        model.types().get(4).fields().stream().map(JavaMappedField::javaType).toList());

    JavaMappedType service = model.types().get(7);
    assertEquals(List.of("g10.Base", "g10.Forward"), service.baseInterfaces());
    assertEquals("int[]", service.attributes().getFirst().javaType());
    assertEquals(
        List.of("java.lang.String[]", "g10.ChoiceHolder", "g10.CountHolder"),
        service.operations().getFirst().parameters().stream()
            .map(JavaMappedParameter::javaType)
            .toList());
    assertEquals("g10.Sequence_string_32__Holder", model.types().getLast().name().qualifiedName());
  }

  @Test
  void mapsG12RicherIdlConstructsForLegacyAndModernModes() {
    IdlSemanticModel semanticModel = semanticModel(g12RicherIdl());

    JavaMappingModel legacy = mapper.map(semanticModel, JavaMappingMode.LEGACY_COMPATIBILITY);
    JavaMappingModel modern = mapper.map(semanticModel, JavaMappingMode.MODERN);

    assertEquals(
        List.of(
            JavaMappedTypeKind.NATIVE,
            JavaMappedTypeKind.EXCEPTION,
            JavaMappedTypeKind.INTERFACE,
            JavaMappedTypeKind.VALUE_BOX,
            JavaMappedTypeKind.VALUETYPE,
            JavaMappedTypeKind.VALUETYPE),
        legacy.types().stream()
            .filter(type -> type.kind() != JavaMappedTypeKind.HOLDER)
            .map(JavaMappedType::kind)
            .toList());
    assertEquals(
        List.of(
            "g12.Handle",
            "g12.Problem",
            "g12.AbstractBase",
            "g12.NameValue",
            "g12.BaseValue",
            "g12.ValueThing"),
        legacy.types().stream()
            .filter(type -> type.kind() != JavaMappedTypeKind.HOLDER)
            .map(type -> type.name().qualifiedName())
            .toList());
    assertEquals(
        List.of(
            "modern.g12.Handle",
            "modern.g12.Problem",
            "modern.g12.AbstractBase",
            "modern.g12.NameValue",
            "modern.g12.BaseValue",
            "modern.g12.ValueThing"),
        modern.types().stream().map(type -> type.name().qualifiedName()).toList());

    JavaMappedType valueBox = legacy.types().get(3);
    assertEquals("java.lang.String", valueBox.aliasType());
    assertEquals(List.of("value"), valueBox.fields().stream().map(JavaMappedField::name).toList());

    JavaMappedType baseValue = legacy.types().get(4);
    assertEquals(true, baseValue.abstractType());
    assertEquals("IDL:example.com/G12/BaseValue:1.0", baseValue.repositoryId());

    JavaMappedType valueThing = legacy.types().get(5);
    assertEquals(List.of("g12.BaseValue"), valueThing.baseInterfaces());
    assertEquals(List.of("g12.AbstractBase"), valueThing.supportedInterfaces());
    assertEquals("IDL:example.com/G12/ValueThing:1.0", valueThing.repositoryId());
    assertEquals(
        List.of("int", "java.lang.String[]"),
        valueThing.fields().stream().map(JavaMappedField::javaType).toList());
    assertEquals(
        List.of("create", "touch"),
        valueThing.operations().stream().map(JavaMappedOperation::name).toList());
    assertEquals(true, valueThing.operations().getFirst().factory());
    assertEquals("g12.ValueThing", valueThing.operations().getFirst().returnType());
    assertEquals("g12.Handle", valueThing.operations().get(1).parameters().getFirst().javaType());
  }

  @Test
  void rejectsCustomValuetypesUntilCustomMarshalingMappingExists() {
    IdlSemanticModel semanticModel =
        semanticModel(
            """
            module Demo {
              custom valuetype CustomValue {
                public long id;
              };
            };
            """);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> mapper.map(semanticModel, JavaMappingMode.LEGACY_COMPATIBILITY));

    assertEquals(
        "Unsupported IDL-to-Java mapping: custom valuetype ::Demo::CustomValue",
        exception.getMessage());
  }

  @Test
  void avoidsConstantHolderNameCollisionsWithGeneratedTypes() {
    JavaMappingModel model =
        mapper.map(
            semanticModel(
                """
                module Demo {
                  const long VALUE = 1;
                  struct IdlConstants { long x; };
                  struct IdlConstants_ { long y; };
                };
                """),
            JavaMappingMode.LEGACY_COMPATIBILITY);

    assertEquals(
        List.of("demo.IdlConstants", "demo.IdlConstants_"),
        model.types().stream().map(type -> type.name().qualifiedName()).toList());
    assertEquals(
        List.of("demo.IdlConstants__"),
        model.constantScopes().stream().map(scope -> scope.name().qualifiedName()).toList());
  }

  private IdlSemanticModel semanticModel(String source) {
    return semanticModel("mapping-test.idl", source);
  }

  private IdlSemanticModel semanticModel(String sourceName, String source) {
    IdlParseResult parseResult = parser.parse(sourceName, source);
    assertFalse(parseResult.hasErrors(), () -> parseResult.diagnostics().toString());
    IdlSemanticResult semanticResult =
        analyzer.analyze(parseResult.translationUnit().orElseThrow());
    assertFalse(semanticResult.hasErrors(), () -> semanticResult.diagnostics().toString());
    return semanticResult.model().orElseThrow();
  }

  private static JavaMappedConstantScope constantScope(
      JavaMappingModel model, String qualifiedName) {
    return model.constantScopes().stream()
        .filter(scope -> scope.name().qualifiedName().equals(qualifiedName))
        .findFirst()
        .orElseThrow();
  }

  private static String rmiGeneratedIdlFixture() {
    try {
      return Files.readString(
          findRepositoryRoot()
              .resolve(
                  "modules/corba-rmi-iiop/src/test/resources/rmi-generated-idl/calculator.idl"));
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to read RMI generated IDL fixture", exception);
    }
  }

  private static String g10PeerStyleIdl() {
    return """
        module G10 {
          const unsigned long LIMIT = 4;
          interface Forward;
          typedef sequence<string<32>, LIMIT> Names;
          typedef long Matrix[2][LIMIT], Count;
          union Choice switch (long) {
            case 0:
            case 1: string<16> text;
            default: Names names;
          };
          exception Problem { string reason; };
          interface Base { void ping(); };
          interface Service : Base, Forward {
            attribute Count counts[LIMIT];
            void submit(in Names names, out Choice result, inout Count count) raises (Problem);
            void collect(out sequence<string<32>> values);
          };
        };
        """;
  }

  private static String g12RicherIdl() {
    return """
        #pragma prefix "example.com"
        module G12 {
          native Handle;
          exception Problem {};
          abstract interface AbstractBase {};
          valuetype NameValue string<32>;
          abstract valuetype BaseValue {};
          valuetype ValueThing : BaseValue supports AbstractBase {
            public long id;
            private sequence<string, 8> names;
            factory create(in long id) raises (Problem);
            void touch(in Handle handle);
          };
          typeprefix BaseValue "example.com/G12";
          typeprefix ValueThing "example.com/G12";
        };
        """;
  }

  private static Path findRepositoryRoot() {
    Path directory = Path.of("").toAbsolutePath().normalize();
    while (directory != null) {
      if (Files.isRegularFile(directory.resolve("AGENT.md"))
          && Files.isDirectory(directory.resolve("modules"))) {
        return directory;
      }
      directory = directory.getParent();
    }
    throw new IllegalStateException("Could not locate repository root from test working directory");
  }
}
