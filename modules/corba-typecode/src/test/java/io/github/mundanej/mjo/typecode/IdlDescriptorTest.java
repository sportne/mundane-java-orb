package io.github.mundanej.mjo.typecode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for static IDL descriptors and compile-only codecs. */
@Tag("unit")
final class IdlDescriptorTest {

  @Test
  void descriptorsExposeDeterministicValuesAndImmutableCollections() {
    RepositoryId repositoryId = RepositoryId.parse("IDL:hello/Greeter:1.0");
    IdlTypeReference stringType =
        new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());
    IdlTypeReference greeterType =
        new IdlTypeReference(
            IdlTypeKind.INTERFACE, "::hello::Greeter", "hello.Greeter", Optional.of(repositoryId));
    IdlOperationDescriptor operation =
        new IdlOperationDescriptor(
            "greet",
            stringType,
            List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, stringType)),
            List.of());
    IdlGeneratedTypeDescriptor descriptor =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.INTERFACE,
            "::hello::Greeter",
            "hello.Greeter",
            repositoryId,
            List.of(),
            List.of(),
            List.of(operation));

    assertEquals(greeterType.repositoryId().orElseThrow(), descriptor.repositoryId());
    assertEquals(List.of(operation), descriptor.operations());
    assertEquals(
        descriptor,
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.INTERFACE,
            "::hello::Greeter",
            "hello.Greeter",
            repositoryId,
            List.of(),
            List.of(),
            List.of(operation)));
    assertThrows(UnsupportedOperationException.class, () -> descriptor.operations().clear());
    assertThrows(IllegalArgumentException.class, () -> new IdlFieldDescriptor(" ", stringType));
  }

  @Test
  void parameterModesAndUnsupportedCodecFailuresAreStable() {
    assertEquals(
        List.of(IdlParameterMode.IN, IdlParameterMode.OUT, IdlParameterMode.INOUT),
        List.of(IdlParameterMode.values()));
    UnsupportedIdlCodec<Object> codec = new UnsupportedIdlCodec<>("deferred codec");

    UnsupportedOperationException readFailure =
        assertThrows(
            UnsupportedOperationException.class,
            () -> codec.read(CdrReader.bigEndian(new byte[0])));
    UnsupportedOperationException writeFailure =
        assertThrows(
            UnsupportedOperationException.class,
            () -> codec.write(CdrWriter.bigEndian(), new Object()));

    assertEquals("deferred codec", readFailure.getMessage());
    assertEquals("deferred codec", writeFailure.getMessage());
    assertThrows(IllegalArgumentException.class, () -> new UnsupportedIdlCodec<>(" "));
    assertThrows(NullPointerException.class, () -> new UnsupportedIdlCodec<>(null));
    assertThrows(NullPointerException.class, () -> codec.read(null));
    assertThrows(NullPointerException.class, () -> codec.write(null, new Object()));
  }

  @Test
  void descriptorValuesRejectBlankNamesAndExposeImmutableCollections() {
    RepositoryId repositoryId = RepositoryId.parse("IDL:demo/Thing:1.0");
    IdlTypeReference longType =
        new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", "int", Optional.empty());
    IdlFieldDescriptor field = new IdlFieldDescriptor("value", longType);
    IdlOperationDescriptor operation =
        new IdlOperationDescriptor("current", longType, List.of(), List.of());
    IdlGeneratedTypeDescriptor descriptor =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.STRUCT,
            "::demo::Thing",
            "demo.Thing",
            repositoryId,
            List.of(field),
            List.of("ONLY"),
            List.of(operation));

    assertThrows(
        IllegalArgumentException.class,
        () -> new IdlTypeReference(IdlTypeKind.PRIMITIVE, " ", "int", Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", " ", Optional.empty()));
    assertThrows(IllegalArgumentException.class, () -> new IdlFieldDescriptor(" ", longType));
    assertThrows(
        IllegalArgumentException.class,
        () -> new IdlParameterDescriptor(" ", IdlParameterMode.IN, longType));
    assertThrows(
        IllegalArgumentException.class,
        () -> new IdlOperationDescriptor(" ", longType, List.of(), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlGeneratedTypeDescriptor(
                IdlTypeKind.STRUCT,
                " ",
                "demo.Thing",
                repositoryId,
                List.of(),
                List.of(),
                List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlGeneratedTypeDescriptor(
                IdlTypeKind.STRUCT,
                "::demo::Thing",
                " ",
                repositoryId,
                List.of(),
                List.of(),
                List.of()));
    assertThrows(UnsupportedOperationException.class, () -> descriptor.fields().clear());
    assertThrows(UnsupportedOperationException.class, () -> descriptor.enumConstants().clear());
  }

  @Test
  void descriptorValueObjectsRejectNullContracts() {
    RepositoryId repositoryId = RepositoryId.parse("IDL:demo/Thing:1.0");
    IdlTypeReference longType =
        new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", "int", Optional.empty());

    assertThrows(
        NullPointerException.class,
        () -> new IdlTypeReference(null, "long", "int", Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () -> new IdlTypeReference(IdlTypeKind.PRIMITIVE, null, "int", Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () -> new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", null, Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () -> new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", "int", null));
    assertThrows(NullPointerException.class, () -> new IdlFieldDescriptor("value", null));
    assertThrows(
        NullPointerException.class, () -> new IdlParameterDescriptor("value", null, longType));
    assertThrows(
        NullPointerException.class,
        () -> new IdlParameterDescriptor("value", IdlParameterMode.IN, null));
    assertThrows(
        NullPointerException.class,
        () -> new IdlOperationDescriptor("current", null, List.of(), List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new IdlOperationDescriptor("current", longType, null, List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new IdlOperationDescriptor("current", longType, List.of(), null));
    assertThrows(
        NullPointerException.class,
        () ->
            new IdlGeneratedTypeDescriptor(
                null,
                "::demo::Thing",
                "demo.Thing",
                repositoryId,
                List.of(),
                List.of(),
                List.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new IdlGeneratedTypeDescriptor(
                IdlTypeKind.STRUCT,
                "::demo::Thing",
                "demo.Thing",
                null,
                List.of(),
                List.of(),
                List.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new IdlGeneratedTypeDescriptor(
                IdlTypeKind.STRUCT,
                "::demo::Thing",
                "demo.Thing",
                repositoryId,
                null,
                List.of(),
                List.of()));
  }

  @Test
  void descriptorCollectionsAreDefensiveCopies() {
    RepositoryId repositoryId = RepositoryId.parse("IDL:demo/Thing:1.0");
    IdlTypeReference longType =
        new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", "int", Optional.empty());
    IdlTypeReference problemType =
        new IdlTypeReference(
            IdlTypeKind.EXCEPTION,
            "::demo::Problem",
            "demo.Problem",
            Optional.of(RepositoryId.parse("IDL:demo/Problem:1.0")));
    IdlParameterDescriptor parameter =
        new IdlParameterDescriptor("value", IdlParameterMode.INOUT, longType);
    ArrayList<IdlParameterDescriptor> parameters = new ArrayList<>(List.of(parameter));
    ArrayList<IdlTypeReference> raises = new ArrayList<>(List.of(problemType));
    IdlOperationDescriptor operation =
        new IdlOperationDescriptor("set", longType, parameters, raises);
    ArrayList<IdlFieldDescriptor> fields =
        new ArrayList<>(List.of(new IdlFieldDescriptor("value", longType)));
    ArrayList<String> enumConstants = new ArrayList<>(List.of("ONE"));
    ArrayList<IdlOperationDescriptor> operations = new ArrayList<>(List.of(operation));
    IdlGeneratedTypeDescriptor descriptor =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.STRUCT,
            "::demo::Thing",
            "demo.Thing",
            repositoryId,
            fields,
            enumConstants,
            operations);

    parameters.clear();
    raises.clear();
    fields.clear();
    enumConstants.clear();
    operations.clear();

    assertEquals(List.of(parameter), operation.parameters());
    assertEquals(List.of(problemType), operation.raises());
    assertEquals(List.of(new IdlFieldDescriptor("value", longType)), descriptor.fields());
    assertEquals(List.of("ONE"), descriptor.enumConstants());
    assertEquals(List.of(operation), descriptor.operations());
    assertThrows(UnsupportedOperationException.class, () -> operation.parameters().clear());
    assertThrows(UnsupportedOperationException.class, () -> operation.raises().clear());
    assertThrows(UnsupportedOperationException.class, () -> descriptor.operations().clear());
  }

  @Test
  void descriptorEnumsRemainInStableOrder() {
    assertEquals(
        List.of(
            IdlTypeKind.VOID,
            IdlTypeKind.PRIMITIVE,
            IdlTypeKind.INTERFACE,
            IdlTypeKind.STRUCT,
            IdlTypeKind.ENUM,
            IdlTypeKind.EXCEPTION),
        List.of(IdlTypeKind.values()));
    assertEquals(
        List.of(IdlParameterMode.IN, IdlParameterMode.OUT, IdlParameterMode.INOUT),
        List.of(IdlParameterMode.values()));
  }
}
