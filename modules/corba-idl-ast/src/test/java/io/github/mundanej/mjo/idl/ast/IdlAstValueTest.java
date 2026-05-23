package io.github.mundanej.mjo.idl.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.common.SourcePosition;
import io.github.mundanej.mjo.common.SourceSpan;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for immutable IDL AST value nodes. */
@Tag("unit")
final class IdlAstValueTest {

  @Test
  void recordsTranslationUnitDeclarationsInEncounterOrder() {
    SourceSpan span = span();
    IdlTypeReference longType = new IdlTypeReference("long", span);
    IdlStruct struct = new IdlStruct("Point", List.of(new IdlField(longType, "x", span)), span);
    IdlModule module = new IdlModule("Geometry", List.of(struct), span);

    IdlTranslationUnit unit = new IdlTranslationUnit(List.of(module), span);

    assertEquals("Geometry", ((IdlModule) unit.declarations().getFirst()).name());
    assertEquals(span, unit.span());
    assertThrows(UnsupportedOperationException.class, () -> unit.declarations().clear());
    assertThrows(UnsupportedOperationException.class, () -> module.declarations().clear());
    assertThrows(UnsupportedOperationException.class, () -> struct.fields().clear());
  }

  @Test
  void recordsInterfaceMembersAndConstantExpressionLexemes() {
    SourceSpan span = span();
    IdlTypeReference stringType = new IdlTypeReference("string", span);
    IdlAttribute attribute = new IdlAttribute(true, stringType, List.of("name"), span);
    IdlOperation operation =
        new IdlOperation(
            false,
            new IdlTypeReference("void", span),
            "rename",
            List.of(
                new IdlParameter(
                    IdlParameterDirection.IN, new IdlTypeReference("string", span), "value", span)),
            List.of("Naming::InvalidName"),
            span);
    IdlInterface idlInterface = new IdlInterface("Named", List.of(attribute, operation), span);
    IdlConstantExpression expression = new IdlConstantExpression(List.of("1", "+", "2"), span);

    assertEquals(List.of(attribute, operation), idlInterface.members());
    assertEquals(List.of("1", "+", "2"), expression.lexemes());
    assertThrows(UnsupportedOperationException.class, () -> attribute.names().clear());
    assertThrows(UnsupportedOperationException.class, () -> operation.parameters().clear());
    assertThrows(UnsupportedOperationException.class, () -> operation.raises().clear());
    assertThrows(UnsupportedOperationException.class, () -> expression.lexemes().clear());
  }

  @Test
  void rejectsBlankNamesAndEmptyRequiredLists() {
    SourceSpan span = span();
    IdlTypeReference longType = new IdlTypeReference("long", span);

    assertThrows(IllegalArgumentException.class, () -> new IdlModule(" ", List.of(), span));
    assertThrows(IllegalArgumentException.class, () -> new IdlInterface("", List.of(), span));
    assertThrows(IllegalArgumentException.class, () -> new IdlField(longType, "\t", span));
    assertThrows(IllegalArgumentException.class, () -> new IdlEnum("Mode", List.of(), span));
    assertThrows(
        IllegalArgumentException.class, () -> new IdlAttribute(false, longType, List.of(), span));
    assertThrows(IllegalArgumentException.class, () -> new IdlConstantExpression(List.of(), span));
  }

  @Test
  void rejectsNullRequiredConstructorComponents() {
    SourceSpan span = span();
    IdlTypeReference longType = new IdlTypeReference("long", span);
    IdlConstantExpression expression = new IdlConstantExpression(List.of("1"), span);

    assertThrows(NullPointerException.class, () -> new IdlTypeReference(null, span));
    assertThrows(NullPointerException.class, () -> new IdlTypeReference("long", null));
    assertThrows(NullPointerException.class, () -> new IdlTranslationUnit(null, span));
    assertThrows(NullPointerException.class, () -> new IdlModule("M", null, span));
    assertThrows(NullPointerException.class, () -> new IdlStruct("S", null, span));
    assertThrows(NullPointerException.class, () -> new IdlEnum("E", null, span));
    assertThrows(NullPointerException.class, () -> new IdlExceptionDeclaration("E", null, span));
    assertThrows(NullPointerException.class, () -> new IdlField(null, "f", span));
    assertThrows(NullPointerException.class, () -> new IdlConstant(null, "C", expression, span));
    assertThrows(NullPointerException.class, () -> new IdlConstant(longType, "C", null, span));
    assertThrows(NullPointerException.class, () -> new IdlConstantExpression(null, span));
    assertThrows(
        NullPointerException.class, () -> new IdlAttribute(false, null, List.of("a"), span));
    assertThrows(NullPointerException.class, () -> new IdlParameter(null, longType, "value", span));
    assertThrows(
        NullPointerException.class,
        () -> new IdlOperation(false, null, "op", List.of(), List.of(), span));
  }

  @Test
  void defensivelyCopiesMutableListsAndRejectsNullElements() {
    SourceSpan span = span();
    IdlTypeReference longType = new IdlTypeReference("long", span);
    IdlStruct struct = new IdlStruct("Point", List.of(new IdlField(longType, "x", span)), span);
    List<IdlDeclaration> declarations = new ArrayList<>();
    declarations.add(struct);
    IdlTranslationUnit unit = new IdlTranslationUnit(declarations, span);

    declarations.clear();

    assertEquals(List.of(struct), unit.declarations());

    List<String> names = new ArrayList<>(List.of("left", "right"));
    IdlAttribute attribute = new IdlAttribute(false, longType, names, span);

    names.add("ignored");

    assertEquals(List.of("left", "right"), attribute.names());

    List<String> enumerators = new ArrayList<>(List.of("ON"));
    enumerators.add(null);
    assertThrows(NullPointerException.class, () -> new IdlEnum("Mode", enumerators, span));

    List<IdlParameter> parameters = new ArrayList<>();
    parameters.add(null);
    assertThrows(
        NullPointerException.class,
        () -> new IdlOperation(false, longType, "op", parameters, List.of(), span));
  }

  @Test
  void recordsHaveValueEquality() {
    SourceSpan span = span();
    IdlEnum first = new IdlEnum("Mode", List.of("ON", "OFF"), span);
    IdlEnum second = new IdlEnum("Mode", List.of("ON", "OFF"), span);

    IdlDeclaration declaration = first;

    assertEquals(first, second);
    assertEquals(first, declaration);
  }

  private static SourceSpan span() {
    SourcePosition start = new SourcePosition("value.idl", 1, 1, 0);
    SourcePosition end = new SourcePosition("value.idl", 1, 1, 0);
    return new SourceSpan(start, end);
  }
}
