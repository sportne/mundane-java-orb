package io.github.mundanej.mjo.repositoryid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RepositoryId}. */
@Tag("unit")
final class RepositoryIdTest {

  @Test
  void parsesAndNormalizesIdlRepositoryIds() {
    RepositoryId objectId = RepositoryId.parse("IDL:omg.org/CORBA/Object:1.0");
    RepositoryId nestedId = RepositoryId.parse("IDL:foo/bar/Baz:2.10");

    assertEquals(RepositoryIdFormat.IDL, objectId.format());
    assertEquals("IDL", objectId.formatName());
    assertEquals("omg.org/CORBA/Object:1.0", objectId.body());
    assertEquals(Optional.of(new RepositoryIdVersion(1, 0)), objectId.version());
    assertEquals("IDL:omg.org/CORBA/Object:1.0", objectId.value());
    assertEquals("IDL:omg.org/CORBA/Object:1.0", objectId.toString());
    assertEquals("IDL:foo/bar/Baz:2.10", nestedId.value());
  }

  @Test
  void rejectsMalformedIdlRepositoryIds() {
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("IDL::1.0"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("IDL:foo//Bar:1.0"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("IDL:foo/bar/:1.0"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("IDL:_foo/bar:1.0"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("IDL:-foo/bar:1.0"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("IDL:.foo/bar:1.0"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("IDL:foo/bar"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("IDL:foo/bar:one.0"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("IDL:foo/bar:-1.0"));
  }

  @Test
  void normalizesIdlVersions() {
    RepositoryId id = RepositoryId.parse("IDL:foo/bar:01.002");

    assertEquals(new RepositoryIdVersion(1, 2), id.version().orElseThrow());
    assertEquals("IDL:foo/bar:1.2", id.value());
  }

  @Test
  void constructsIdlRepositoryIdsFromPathAndScopedNames() {
    RepositoryId prefixed =
        RepositoryId.idl(
            Optional.of("omg.org"), List.of("CORBA", "Object"), new RepositoryIdVersion(1, 0));
    RepositoryId scoped =
        RepositoryId.idl(List.of("foo", "bar", "Baz"), new RepositoryIdVersion(2, 10));

    assertEquals("IDL:omg.org/CORBA/Object:1.0", prefixed.value());
    assertEquals("IDL:foo/bar/Baz:2.10", scoped.value());
  }

  @Test
  void rejectsInvalidIdlBuilderInputs() {
    assertThrows(
        RepositoryIdException.class,
        () -> RepositoryId.idl(List.of(), new RepositoryIdVersion(1, 0)));
    assertThrows(
        RepositoryIdException.class,
        () -> RepositoryId.idl(List.of("foo", "bad segment"), new RepositoryIdVersion(1, 0)));
  }

  @Test
  void preservesRecognizedNonIdlRepositoryIds() {
    assertPreserved("RMI:example.Widget:1234567812345678", RepositoryIdFormat.RMI);
    assertPreserved("RMI:example.Widget:1234567812345678:ABCD123456781234", RepositoryIdFormat.RMI);
    assertPreserved("DCE:700dc518-0110-11ce-ac8f-0800090b5d3e:1", RepositoryIdFormat.DCE);
    assertPreserved("LOCAL:transient-local-object", RepositoryIdFormat.LOCAL);
  }

  @Test
  void rejectsMalformedRecognizedNonIdlRepositoryIds() {
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("RMI:example.Widget"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("RMI:example.Widget::hash"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("DCE:not-a-uuid:1"));
  }

  @Test
  void preservesUnknownRepositoryIdFormats() {
    RepositoryId id = RepositoryId.parse("CUSTOM:anything:with:colons");

    assertEquals(RepositoryIdFormat.UNKNOWN, id.format());
    assertEquals("CUSTOM", id.formatName());
    assertEquals("anything:with:colons", id.body());
    assertFalse(id.version().isPresent());
    assertEquals("CUSTOM:anything:with:colons", id.value());
  }

  @Test
  void rejectsRepositoryIdsWithoutValidFormat() {
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse("IDL"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.parse(":body"));
    assertThrows(RepositoryIdException.class, () -> RepositoryId.generic("A:B", "body"));
  }

  @Test
  void equalityUsesDeterministicValue() {
    RepositoryId parsed = RepositoryId.parse("IDL:foo:01.002");
    RepositoryId constructed = RepositoryId.idl("foo", new RepositoryIdVersion(1, 2));

    assertEquals(parsed, constructed);
    assertEquals(parsed.hashCode(), constructed.hashCode());
  }

  private static void assertPreserved(String value, RepositoryIdFormat format) {
    RepositoryId id = RepositoryId.parse(value);

    assertEquals(format, id.format());
    assertEquals(value, id.value());
    assertTrue(id.version().isEmpty());
  }
}
