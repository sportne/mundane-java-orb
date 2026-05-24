package io.github.mundanej.mjo.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlFieldDescriptor;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import io.github.mundanej.mjo.typecode.IdlTypeCodeKind;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for explicit static Interface Repository metadata. */
@Tag("unit")
final class StaticInterfaceRepositoryTest {

  private static final IdlTypeReference LONG =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", "int", Optional.empty());
  private static final IdlTypeReference VOID =
      new IdlTypeReference(IdlTypeKind.VOID, "void", "void", Optional.empty());
  private static final RepositoryId COLOR_ID = RepositoryId.parse("IDL:demo/Color:1.0");
  private static final RepositoryId POINT_ID = RepositoryId.parse("IDL:demo/Point:1.0");
  private static final RepositoryId SHAPE_ID = RepositoryId.parse("IDL:demo/Shape:1.0");
  private static final IdlTypeReference COLOR_REFERENCE =
      new IdlTypeReference(IdlTypeKind.ENUM, "::demo::Color", "demo.Color", Optional.of(COLOR_ID));
  private static final IdlTypeReference POINT_REFERENCE =
      new IdlTypeReference(
          IdlTypeKind.STRUCT, "::demo::Point", "demo.Point", Optional.of(POINT_ID));

  @Test
  void indexesDescriptorsByRepositoryIdIdlNameAndJavaName() {
    StaticInterfaceRepository repository = repository();

    assertEquals(List.of(color(), point(), shape()), repository.descriptors());
    assertEquals(color(), repository.findByRepositoryId(COLOR_ID).orElseThrow());
    assertEquals(point(), repository.findByIdlScopedName("::demo::Point").orElseThrow());
    assertEquals(shape(), repository.findByJavaName("demo.Shape").orElseThrow());
    assertEquals(shape(), repository.requireByRepositoryId(SHAPE_ID));
    assertEquals("draw", repository.requireOperation(SHAPE_ID, "draw").name());
  }

  @Test
  void emptyRepositoryLookupsReturnEmptyAndRequireFailsDeterministically() {
    StaticInterfaceRepository repository = StaticInterfaceRepository.of(List.of());
    RepositoryId missing = RepositoryId.parse("IDL:demo/Missing:1.0");

    assertEquals(Optional.empty(), repository.findByRepositoryId(missing));
    assertEquals(Optional.empty(), repository.findByIdlScopedName("::demo::Missing"));
    assertEquals(Optional.empty(), repository.findByJavaName("demo.Missing"));

    InterfaceRepositoryException exception =
        assertThrows(
            InterfaceRepositoryException.class, () -> repository.requireByRepositoryId(missing));

    assertEquals(InterfaceRepositoryDiagnosticCodes.MISSING_DESCRIPTOR, exception.code());
  }

  @Test
  void lookupInputsRejectNullAndBlankNames() {
    StaticInterfaceRepository repository = repository();

    assertThrows(NullPointerException.class, () -> StaticInterfaceRepository.of(null));
    assertThrows(
        NullPointerException.class,
        () -> StaticInterfaceRepository.of(java.util.Collections.singletonList(null)));
    assertThrows(NullPointerException.class, () -> repository.findByRepositoryId(null));
    assertThrows(NullPointerException.class, () -> repository.findByIdlScopedName(null));
    assertThrows(NullPointerException.class, () -> repository.findByJavaName(null));
    assertThrows(NullPointerException.class, () -> repository.requireOperation(SHAPE_ID, null));
    assertThrows(IllegalArgumentException.class, () -> repository.findByIdlScopedName(" "));
    assertThrows(IllegalArgumentException.class, () -> repository.findByJavaName(" "));
    assertThrows(IllegalArgumentException.class, () -> repository.requireOperation(SHAPE_ID, " "));
  }

  @Test
  void returnedDescriptorOrderIsImmutable() {
    StaticInterfaceRepository repository = repository();

    assertThrows(UnsupportedOperationException.class, () -> repository.descriptors().add(color()));
  }

  @Test
  void buildsTypeCodesThroughRepositoryBackedReferenceResolution() {
    StaticInterfaceRepository repository = repository();

    IdlTypeCode pointType = repository.typeCode(point());
    IdlTypeCode shapeType = repository.typeCode(shape());

    assertEquals(IdlTypeCodeKind.STRUCT, pointType.kind());
    assertEquals(
        List.of("x", "color"), pointType.members().stream().map(member -> member.name()).toList());
    assertEquals(IdlTypeCodeKind.ENUM, pointType.members().get(1).type().kind());
    assertEquals(IdlTypeCodeKind.INTERFACE, shapeType.kind());
  }

  @Test
  void missingGeneratedReferencesFailDeterministically() {
    IdlGeneratedTypeDescriptor broken =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.STRUCT,
            "::demo::Broken",
            "demo.Broken",
            RepositoryId.parse("IDL:demo/Broken:1.0"),
            List.of(
                new IdlFieldDescriptor(
                    "missing",
                    new IdlTypeReference(
                        IdlTypeKind.STRUCT,
                        "::demo::Missing",
                        "demo.Missing",
                        Optional.of(RepositoryId.parse("IDL:demo/Missing:1.0"))))),
            List.of(),
            List.of());
    StaticInterfaceRepository repository = StaticInterfaceRepository.of(List.of(broken));

    InterfaceRepositoryException exception =
        assertThrows(InterfaceRepositoryException.class, () -> repository.typeCode(broken));

    assertEquals(InterfaceRepositoryDiagnosticCodes.MISSING_DESCRIPTOR, exception.code());
  }

  @Test
  void generatedReferencesWithoutRepositoryIdsFailDeterministically() {
    IdlGeneratedTypeDescriptor broken =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.STRUCT,
            "::demo::Broken",
            "demo.Broken",
            RepositoryId.parse("IDL:demo/Broken:1.0"),
            List.of(
                new IdlFieldDescriptor(
                    "missing",
                    new IdlTypeReference(
                        IdlTypeKind.STRUCT, "::demo::Missing", "demo.Missing", Optional.empty()))),
            List.of(),
            List.of());
    StaticInterfaceRepository repository = StaticInterfaceRepository.of(List.of(broken));

    InterfaceRepositoryException exception =
        assertThrows(InterfaceRepositoryException.class, () -> repository.typeCode(broken));

    assertEquals(InterfaceRepositoryDiagnosticCodes.INVALID_REFERENCE, exception.code());
  }

  @Test
  void duplicateDescriptorKeysFailDeterministically() {
    IdlGeneratedTypeDescriptor duplicateRepositoryId =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.ENUM,
            "::demo::OtherColor",
            "demo.OtherColor",
            COLOR_ID,
            List.of(),
            List.of("BLUE"),
            List.of());
    IdlGeneratedTypeDescriptor duplicateIdlName =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.ENUM,
            "::demo::Color",
            "demo.OtherColor",
            RepositoryId.parse("IDL:demo/OtherColor:1.0"),
            List.of(),
            List.of("BLUE"),
            List.of());
    IdlGeneratedTypeDescriptor duplicateJavaName =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.ENUM,
            "::demo::OtherColor",
            "demo.Color",
            RepositoryId.parse("IDL:demo/OtherColor:1.0"),
            List.of(),
            List.of("BLUE"),
            List.of());

    InterfaceRepositoryException duplicateRepository =
        assertThrows(
            InterfaceRepositoryException.class,
            () -> StaticInterfaceRepository.of(List.of(color(), duplicateRepositoryId)));
    InterfaceRepositoryException duplicateIdl =
        assertThrows(
            InterfaceRepositoryException.class,
            () -> StaticInterfaceRepository.of(List.of(color(), duplicateIdlName)));
    InterfaceRepositoryException duplicateJava =
        assertThrows(
            InterfaceRepositoryException.class,
            () -> StaticInterfaceRepository.of(List.of(color(), duplicateJavaName)));

    assertEquals(
        InterfaceRepositoryDiagnosticCodes.DUPLICATE_DESCRIPTOR, duplicateRepository.code());
    assertEquals(InterfaceRepositoryDiagnosticCodes.DUPLICATE_DESCRIPTOR, duplicateIdl.code());
    assertEquals(InterfaceRepositoryDiagnosticCodes.DUPLICATE_DESCRIPTOR, duplicateJava.code());
  }

  @Test
  void unsupportedDescriptorKindsFailDeterministically() {
    IdlGeneratedTypeDescriptor primitive =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.PRIMITIVE,
            "long",
            "int",
            RepositoryId.parse("LOCAL:long"),
            List.of(),
            List.of(),
            List.of());

    InterfaceRepositoryException exception =
        assertThrows(
            InterfaceRepositoryException.class,
            () -> StaticInterfaceRepository.of(List.of(primitive)));

    assertEquals(InterfaceRepositoryDiagnosticCodes.UNSUPPORTED_DESCRIPTOR_KIND, exception.code());
  }

  @Test
  void missingDescriptorsAndOperationsFailDeterministically() {
    StaticInterfaceRepository repository = repository();

    InterfaceRepositoryException missingDescriptor =
        assertThrows(
            InterfaceRepositoryException.class,
            () -> repository.requireByRepositoryId(RepositoryId.parse("IDL:demo/Missing:1.0")));
    InterfaceRepositoryException missingOperation =
        assertThrows(
            InterfaceRepositoryException.class,
            () -> repository.requireOperation(SHAPE_ID, "missing"));

    assertEquals(InterfaceRepositoryDiagnosticCodes.MISSING_DESCRIPTOR, missingDescriptor.code());
    assertEquals(InterfaceRepositoryDiagnosticCodes.MISSING_DESCRIPTOR, missingOperation.code());
  }

  @Test
  void typeCodeRejectsUnregisteredDescriptorWithKnownRepositoryId() {
    StaticInterfaceRepository repository = repository();
    IdlGeneratedTypeDescriptor conflictingPoint =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.STRUCT,
            "::demo::Point",
            "demo.Point",
            POINT_ID,
            List.of(new IdlFieldDescriptor("different", LONG)),
            List.of(),
            List.of());

    InterfaceRepositoryException exception =
        assertThrows(
            InterfaceRepositoryException.class, () -> repository.typeCode(conflictingPoint));

    assertEquals(InterfaceRepositoryDiagnosticCodes.INVALID_REFERENCE, exception.code());
  }

  @Test
  void recursiveAggregateTypeCodesFailWithBoundedDiagnostic() {
    RepositoryId nodeId = RepositoryId.parse("IDL:demo/Node:1.0");
    IdlGeneratedTypeDescriptor recursive =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.STRUCT,
            "::demo::Node",
            "demo.Node",
            nodeId,
            List.of(
                new IdlFieldDescriptor(
                    "next",
                    new IdlTypeReference(
                        IdlTypeKind.STRUCT, "::demo::Node", "demo.Node", Optional.of(nodeId)))),
            List.of(),
            List.of());
    StaticInterfaceRepository repository = StaticInterfaceRepository.of(List.of(recursive));

    InterfaceRepositoryException exception =
        assertThrows(InterfaceRepositoryException.class, () -> repository.typeCode(recursive));

    assertEquals(InterfaceRepositoryDiagnosticCodes.INVALID_REFERENCE, exception.code());
  }

  private static StaticInterfaceRepository repository() {
    return StaticInterfaceRepository.of(List.of(color(), point(), shape()));
  }

  private static IdlGeneratedTypeDescriptor color() {
    return new IdlGeneratedTypeDescriptor(
        IdlTypeKind.ENUM,
        "::demo::Color",
        "demo.Color",
        COLOR_ID,
        List.of(),
        List.of("RED", "GREEN"),
        List.of());
  }

  private static IdlGeneratedTypeDescriptor point() {
    return new IdlGeneratedTypeDescriptor(
        IdlTypeKind.STRUCT,
        "::demo::Point",
        "demo.Point",
        POINT_ID,
        List.of(
            new IdlFieldDescriptor("x", LONG), new IdlFieldDescriptor("color", COLOR_REFERENCE)),
        List.of(),
        List.of());
  }

  private static IdlGeneratedTypeDescriptor shape() {
    IdlOperationDescriptor draw =
        new IdlOperationDescriptor(
            "draw",
            VOID,
            List.of(new IdlParameterDescriptor("point", IdlParameterMode.IN, POINT_REFERENCE)),
            List.of());
    return new IdlGeneratedTypeDescriptor(
        IdlTypeKind.INTERFACE,
        "::demo::Shape",
        "demo.Shape",
        SHAPE_ID,
        List.of(),
        List.of(),
        List.of(draw));
  }
}
