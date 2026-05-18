package io.github.mundanej.mjo.typecode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.repositoryid.RepositoryId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for descriptor-backed local TypeCode metadata. */
@Tag("unit")
final class IdlTypeCodeTest {

  @Test
  void primitiveReferencesMapToStableScalarTypeCodes() {
    assertEquals(IdlTypeCode.VOID, primitiveReference("void", "void"));
    assertEquals(IdlTypeCode.BOOLEAN, primitiveReference("boolean", "boolean"));
    assertEquals(IdlTypeCode.OCTET, primitiveReference("octet", "int"));
    assertEquals(IdlTypeCode.CHAR, primitiveReference("char", "char"));
    assertEquals(IdlTypeCode.SHORT, primitiveReference("short", "short"));
    assertEquals(IdlTypeCode.UNSIGNED_SHORT, primitiveReference("unsigned short", "int"));
    assertEquals(IdlTypeCode.LONG, primitiveReference("long", "int"));
    assertEquals(IdlTypeCode.UNSIGNED_LONG, primitiveReference("unsigned long", "long"));
    assertEquals(IdlTypeCode.LONG_LONG, primitiveReference("long long", "long"));
    assertEquals(
        IdlTypeCode.UNSIGNED_LONG_LONG,
        primitiveReference("unsigned long long", "java.math.BigInteger"));
    assertEquals(IdlTypeCode.FLOAT, primitiveReference("float", "float"));
    assertEquals(IdlTypeCode.DOUBLE, primitiveReference("double", "double"));
    assertEquals(IdlTypeCode.LONG_DOUBLE, primitiveReference("long double", "byte[]"));
    assertEquals(IdlTypeCode.STRING, primitiveReference("string", "java.lang.String"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            IdlTypeCode.fromTypeReference(
                new IdlTypeReference(
                    IdlTypeKind.PRIMITIVE, "wstring", "java.lang.String", Optional.empty())));
  }

  @Test
  void generatedStructDescriptorPreservesMemberOrderAndRepositoryId() {
    RepositoryId repositoryId = RepositoryId.parse("IDL:demo/Point:1.0");
    IdlTypeReference longType =
        new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", "int", Optional.empty());
    IdlGeneratedTypeDescriptor descriptor =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.STRUCT,
            "::demo::Point",
            "demo.Point",
            repositoryId,
            List.of(new IdlFieldDescriptor("x", longType), new IdlFieldDescriptor("y", longType)),
            List.of(),
            List.of());

    IdlTypeCode typeCode = IdlTypeCode.fromDescriptor(descriptor);

    assertEquals(IdlTypeCodeKind.STRUCT, typeCode.kind());
    assertTrue(typeCode.isAggregate());
    assertEquals(Optional.of(repositoryId), typeCode.repositoryId());
    assertEquals(
        List.of("x", "y"), typeCode.members().stream().map(IdlTypeCodeMember::name).toList());
    assertEquals(List.of(IdlTypeCode.LONG, IdlTypeCode.LONG), memberTypes(typeCode));
    assertThrows(UnsupportedOperationException.class, () -> typeCode.members().clear());
  }

  @Test
  void generatedEnumAndExceptionDescriptorsExposeExpectedShapes() {
    RepositoryId enumId = RepositoryId.parse("IDL:demo/Color:1.0");
    IdlTypeCode enumType =
        IdlTypeCode.fromDescriptor(
            new IdlGeneratedTypeDescriptor(
                IdlTypeKind.ENUM,
                "::demo::Color",
                "demo.Color",
                enumId,
                List.of(),
                List.of("RED", "GREEN"),
                List.of()));
    RepositoryId exceptionId = RepositoryId.parse("IDL:demo/Problem:1.0");
    IdlTypeReference stringType =
        new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());
    IdlTypeCode exceptionType =
        IdlTypeCode.fromDescriptor(
            new IdlGeneratedTypeDescriptor(
                IdlTypeKind.EXCEPTION,
                "::demo::Problem",
                "demo.Problem",
                exceptionId,
                List.of(new IdlFieldDescriptor("message", stringType)),
                List.of(),
                List.of()));

    assertEquals(IdlTypeCodeKind.ENUM, enumType.kind());
    assertEquals(List.of("RED", "GREEN"), enumType.enumConstants());
    assertEquals(IdlTypeCodeKind.EXCEPTION, exceptionType.kind());
    assertTrue(exceptionType.isAggregate());
    assertEquals(
        List.of("message"), exceptionType.members().stream().map(IdlTypeCodeMember::name).toList());
  }

  @Test
  void sequenceTypeCodeCarriesElementTypeAndImmutableShape() {
    IdlTypeCode sequence = IdlTypeCode.sequenceOf(IdlTypeCode.STRING, "sequence<string>", "List");

    assertEquals(IdlTypeCodeKind.SEQUENCE, sequence.kind());
    assertEquals(Optional.of(IdlTypeCode.STRING), sequence.elementType());
    assertThrows(IllegalArgumentException.class, () -> IdlTypeCode.sequenceOf(null, "seq", "List"));
  }

  @Test
  void generatedReferencesRequireRepositoryIdsAndSupportInterfaces() {
    RepositoryId repositoryId = RepositoryId.parse("IDL:demo/Service:1.0");
    IdlTypeCode interfaceType =
        IdlTypeCode.fromTypeReference(
            new IdlTypeReference(
                IdlTypeKind.INTERFACE,
                "::demo::Service",
                "demo.Service",
                Optional.of(repositoryId)));

    assertEquals(IdlTypeCodeKind.INTERFACE, interfaceType.kind());
    assertEquals(Optional.of(repositoryId), interfaceType.repositoryId());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            IdlTypeCode.fromTypeReference(
                new IdlTypeReference(
                    IdlTypeKind.INTERFACE, "::demo::Service", "demo.Service", Optional.empty())));
  }

  @Test
  void descriptorConversionResolvesUserDefinedFieldReferencesThroughCallerResolver() {
    RepositoryId colorId = RepositoryId.parse("IDL:demo/Color:1.0");
    IdlTypeCode colorType =
        IdlTypeCode.fromDescriptor(
            new IdlGeneratedTypeDescriptor(
                IdlTypeKind.ENUM,
                "::demo::Color",
                "demo.Color",
                colorId,
                List.of(),
                List.of("RED", "GREEN"),
                List.of()));
    IdlTypeReference colorReference =
        new IdlTypeReference(IdlTypeKind.ENUM, "::demo::Color", "demo.Color", Optional.of(colorId));
    IdlGeneratedTypeDescriptor swatch =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.STRUCT,
            "::demo::Swatch",
            "demo.Swatch",
            RepositoryId.parse("IDL:demo/Swatch:1.0"),
            List.of(new IdlFieldDescriptor("color", colorReference)),
            List.of(),
            List.of());

    IllegalArgumentException unresolved =
        assertThrows(IllegalArgumentException.class, () -> IdlTypeCode.fromDescriptor(swatch));
    IdlTypeCode resolved =
        IdlTypeCode.fromDescriptor(
            swatch,
            reference ->
                reference.repositoryId().filter(colorId::equals).isPresent()
                    ? colorType
                    : IdlTypeCode.fromTypeReference(reference));

    assertEquals(
        "generated aggregate TypeCode reference requires a descriptor resolver: ::demo::Color",
        unresolved.getMessage());
    assertEquals(List.of(colorType), memberTypes(resolved));
  }

  @Test
  void invalidTypeCodeCombinationsFailDeterministically() {
    RepositoryId repositoryId = RepositoryId.parse("IDL:demo/Bad:1.0");
    IdlTypeCodeMember member = new IdlTypeCodeMember("value", IdlTypeCode.LONG);

    assertThrows(
        IllegalArgumentException.class,
        () -> IdlTypeCode.primitive(IdlTypeCodeKind.SEQUENCE, "sequence<long>", "List"));
    assertThrows(
        IllegalArgumentException.class,
        () -> IdlTypeCode.primitive(IdlTypeCodeKind.ENUM, "::demo::Bad", "demo.Bad"));
    assertThrows(
        IllegalArgumentException.class,
        () -> IdlTypeCode.primitive(IdlTypeCodeKind.STRUCT, "::demo::Bad", "demo.Bad"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlTypeCode(
                IdlTypeCodeKind.STRUCT,
                "::demo::Bad",
                "demo.Bad",
                Optional.of(repositoryId),
                List.of(),
                List.of(),
                Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlTypeCode(
                IdlTypeCodeKind.STRUCT,
                "::demo::Bad",
                "demo.Bad",
                Optional.empty(),
                List.of(member),
                List.of(),
                Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlTypeCode(
                IdlTypeCodeKind.STRUCT,
                "::demo::Bad",
                "demo.Bad",
                Optional.of(repositoryId),
                List.of(member),
                List.of("BAD"),
                Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlTypeCode(
                IdlTypeCodeKind.ENUM,
                "::demo::Bad",
                "demo.Bad",
                Optional.of(repositoryId),
                List.of(),
                List.of(),
                Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlTypeCode(
                IdlTypeCodeKind.ENUM,
                "::demo::Bad",
                "demo.Bad",
                Optional.of(repositoryId),
                List.of(member),
                List.of("BAD"),
                Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlTypeCode(
                IdlTypeCodeKind.SEQUENCE,
                "sequence<long>",
                "List",
                Optional.empty(),
                List.of(),
                List.of(),
                Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlTypeCode(
                IdlTypeCodeKind.SEQUENCE,
                "sequence<long>",
                "List",
                Optional.of(repositoryId),
                List.of(),
                List.of(),
                Optional.of(IdlTypeCode.LONG)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlTypeCode(
                IdlTypeCodeKind.INTERFACE,
                "::demo::Bad",
                "demo.Bad",
                Optional.of(repositoryId),
                List.of(member),
                List.of(),
                Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlTypeCode(
                IdlTypeCodeKind.LONG,
                "long",
                "int",
                Optional.of(repositoryId),
                List.of(),
                List.of(),
                Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlTypeCode(
                IdlTypeCodeKind.LONG,
                "long",
                "int",
                Optional.empty(),
                List.of(member),
                List.of(),
                Optional.empty()));
    assertTrue(IdlTypeCode.fromDescriptor(interfaceDescriptor()).repositoryId().isPresent());
  }

  private static IdlTypeCode primitiveReference(String idlName, String javaName) {
    return IdlTypeCode.fromTypeReference(
        new IdlTypeReference(IdlTypeKind.PRIMITIVE, idlName, javaName, Optional.empty()));
  }

  private static IdlGeneratedTypeDescriptor interfaceDescriptor() {
    return new IdlGeneratedTypeDescriptor(
        IdlTypeKind.INTERFACE,
        "::demo::Service",
        "demo.Service",
        RepositoryId.parse("IDL:demo/Service:1.0"),
        List.of(),
        List.of(),
        List.of());
  }

  private static List<IdlTypeCode> memberTypes(IdlTypeCode typeCode) {
    return typeCode.members().stream().map(IdlTypeCodeMember::type).toList();
  }
}
