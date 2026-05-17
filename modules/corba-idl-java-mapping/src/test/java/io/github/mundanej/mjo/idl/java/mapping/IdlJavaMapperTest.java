package io.github.mundanej.mjo.idl.java.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.idl.parser.IdlParseResult;
import io.github.mundanej.mjo.idl.parser.IdlParser;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticAnalyzer;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticModel;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticResult;
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
        legacy.types().stream().map(type -> type.name().qualifiedName()).toList());
    assertEquals(
        List.of("demo.IdlConstants"),
        legacy.constantScopes().stream().map(scope -> scope.name().qualifiedName()).toList());
    assertEquals(
        List.of("modern.demo.Color", "modern.demo.Point", "modern.demo.Bad", "modern.demo.Shape"),
        modern.types().stream().map(type -> type.name().qualifiedName()).toList());

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
    IdlParseResult parseResult = parser.parse("mapping-test.idl", source);
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
}
