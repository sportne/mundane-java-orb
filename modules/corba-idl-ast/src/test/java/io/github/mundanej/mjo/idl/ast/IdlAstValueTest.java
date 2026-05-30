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

  @Test
  void recordsG10GrammarValueNodes() {
    SourceSpan span = span();
    IdlConstantExpression four = new IdlConstantExpression(List.of("4"), span);
    IdlArrayDimension dimension = new IdlArrayDimension(four, span);
    IdlDeclarator array = new IdlDeclarator("values", List.of(dimension), span);
    IdlTypeReference boundedString = IdlTypeReference.boundedString("string", four, span);
    IdlTypeReference sequence = IdlTypeReference.sequence(boundedString, four, span);
    IdlTypedef typedef = new IdlTypedef(sequence, List.of(array), span);
    IdlUnionLabel label = IdlUnionLabel.caseLabel(four, span);
    IdlUnionCase unionCase = new IdlUnionCase(List.of(label), sequence, array, span);
    IdlUnion union =
        new IdlUnion("Choice", new IdlTypeReference("long", span), List.of(unionCase), span);
    IdlInterfaceForward forward = new IdlInterfaceForward("Forward", span);
    IdlInterface child = new IdlInterface("Child", List.of("Forward"), List.of(), span);

    assertEquals("sequence<string<4>, 4>", typedef.type().name());
    assertEquals(List.of(dimension), array.dimensions());
    assertEquals("Choice", union.name());
    assertEquals(false, label.defaultLabel());
    assertEquals("Forward", forward.name());
    assertEquals(List.of("Forward"), child.baseInterfaces());
    assertThrows(UnsupportedOperationException.class, () -> typedef.declarators().clear());
    assertThrows(UnsupportedOperationException.class, () -> union.cases().clear());
    assertThrows(UnsupportedOperationException.class, () -> child.baseInterfaces().clear());
  }

  @Test
  void recordsG12GrammarValueNodes() {
    SourceSpan span = span();
    IdlConstantExpression eight = new IdlConstantExpression(List.of("8"), span);
    IdlTypeReference string = new IdlTypeReference("string", span);
    IdlTypeReference boundedString = IdlTypeReference.boundedString("string", eight, span);
    IdlDeclarator id = new IdlDeclarator("id", span);
    IdlValueField publicField =
        new IdlValueField(
            IdlValueVisibility.PUBLIC, new IdlTypeReference("long", span), List.of(id), span);
    IdlValueFactory factory =
        new IdlValueFactory(
            "create",
            List.of(new IdlParameter(IdlParameterDirection.IN, string, "name", span)),
            List.of("Problem"),
            span);
    IdlOperation operation =
        new IdlOperation(
            false,
            new IdlTypeReference("void", span),
            "touch",
            List.of(),
            List.of(),
            List.of("\"tenant\""),
            span);
    IdlValueType valueType =
        new IdlValueType(
            false,
            false,
            "Holder",
            List.of("BaseValue"),
            List.of("AbstractBase"),
            List.of(publicField, factory, operation),
            span);

    assertEquals(
        IdlInterfaceKind.ABSTRACT,
        new IdlInterfaceForward(IdlInterfaceKind.ABSTRACT, "I", span).kind());
    assertEquals("Handle", new IdlNative("Handle", span).name());
    assertEquals("prefix", new IdlPragma("prefix", List.of("\"example\""), span).name());
    assertEquals("NameValue", new IdlValueBox("NameValue", boundedString, span).name());
    assertEquals("ForwardValue", new IdlValueTypeForward(false, "ForwardValue", span).name());
    assertEquals(List.of("BaseValue"), valueType.baseValueTypes());
    assertEquals(List.of("AbstractBase"), valueType.supportedInterfaces());
    assertEquals(List.of(publicField, factory, operation), valueType.members());
    assertEquals(List.of("\"tenant\""), operation.contexts());
    assertThrows(UnsupportedOperationException.class, () -> valueType.members().clear());
    assertThrows(UnsupportedOperationException.class, () -> publicField.declarators().clear());
    assertThrows(UnsupportedOperationException.class, () -> factory.parameters().clear());
  }

  @Test
  void rejectsInvalidG10GrammarValueNodes() {
    SourceSpan span = span();
    IdlConstantExpression one = new IdlConstantExpression(List.of("1"), span);
    IdlTypeReference longType = new IdlTypeReference("long", span);
    IdlDeclarator declarator = new IdlDeclarator("value", span);
    IdlUnionCase validCase =
        new IdlUnionCase(List.of(IdlUnionLabel.caseLabel(one, span)), longType, declarator, span);

    assertThrows(NullPointerException.class, () -> new IdlArrayDimension(null, span));
    assertThrows(NullPointerException.class, () -> new IdlArrayDimension(one, null));
    assertThrows(IllegalArgumentException.class, () -> new IdlDeclarator(" ", span));
    assertThrows(NullPointerException.class, () -> new IdlDeclarator("value", null, span));
    assertThrows(NullPointerException.class, () -> new IdlDeclarator("value", List.of(), null));
    assertThrows(IllegalArgumentException.class, () -> new IdlInterfaceForward("", span));
    assertThrows(NullPointerException.class, () -> new IdlInterfaceForward("Forward", null));
    assertThrows(NullPointerException.class, () -> new IdlTypedef(null, List.of(declarator), span));
    assertThrows(IllegalArgumentException.class, () -> new IdlTypedef(longType, List.of(), span));
    assertThrows(
        NullPointerException.class, () -> new IdlTypedef(longType, List.of(declarator), null));
    assertThrows(
        IllegalArgumentException.class, () -> new IdlUnion(" ", longType, List.of(), span));
    assertThrows(
        NullPointerException.class, () -> new IdlUnion("U", null, List.of(validCase), span));
    assertThrows(
        IllegalArgumentException.class, () -> new IdlUnion("U", longType, List.of(), span));
    assertThrows(
        NullPointerException.class, () -> new IdlUnion("U", longType, List.of(validCase), null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new IdlUnionCase(List.of(), longType, declarator, span));
    assertThrows(
        NullPointerException.class,
        () ->
            new IdlUnionCase(List.of(IdlUnionLabel.caseLabel(one, span)), null, declarator, span));
    assertThrows(
        NullPointerException.class,
        () -> new IdlUnionCase(List.of(IdlUnionLabel.caseLabel(one, span)), longType, null, span));
    assertThrows(
        NullPointerException.class,
        () ->
            new IdlUnionCase(
                List.of(IdlUnionLabel.caseLabel(one, span)), longType, declarator, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new IdlUnionLabel(true, java.util.Optional.of(one), span));
    assertThrows(
        IllegalArgumentException.class,
        () -> new IdlUnionLabel(false, java.util.Optional.empty(), span));
    assertThrows(
        IllegalArgumentException.class, () -> IdlTypeReference.boundedString("char", one, span));
    assertThrows(NullPointerException.class, () -> IdlTypeReference.sequence(null, span));
    assertThrows(NullPointerException.class, () -> IdlTypeReference.sequence(longType, null, span));
    assertThrows(NullPointerException.class, () -> IdlTypeReference.sequence(longType, one, null));
  }

  @Test
  void g10GrammarValuesExposeEqualityBranches() {
    SourceSpan span = span();
    IdlTypeReference first = IdlTypeReference.sequence(new IdlTypeReference("long", span), span);
    IdlTypeReference second = IdlTypeReference.sequence(new IdlTypeReference("long", span), span);
    IdlAttribute attribute = new IdlAttribute(false, first, List.of("values"), span);

    assertEquals(first, first);
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertEquals(false, first.equals(new IdlTypeReference("short", span)));
    assertEquals(false, attribute.declarators().getFirst().array());
    assertEquals(attribute, new IdlAttribute(false, first, List.of("values"), span));
    assertEquals(
        attribute.hashCode(), new IdlAttribute(false, first, List.of("values"), span).hashCode());
  }

  private static SourceSpan span() {
    SourcePosition start = new SourcePosition("value.idl", 1, 1, 0);
    SourcePosition end = new SourcePosition("value.idl", 1, 1, 0);
    return new SourceSpan(start, end);
  }
}
