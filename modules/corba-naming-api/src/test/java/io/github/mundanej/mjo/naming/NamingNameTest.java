package io.github.mundanej.mjo.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for local CosNaming stringified names. */
@Tag("unit")
final class NamingNameTest {

  @Test
  void parsesSlashSeparatedComponentsAndDotSeparatedKind() {
    NamingName name = NamingName.parse("app/service.kind/leaf");

    assertEquals(
        List.of(
            new NameComponent("app", ""),
            new NameComponent("service", "kind"),
            new NameComponent("leaf", "")),
        name.components());
    assertEquals("app/service.kind/leaf", name.stringified());
    assertEquals(name.stringified(), name.toString());
    assertEquals(new NameComponent("leaf", ""), name.leaf());
    assertEquals(
        List.of(new NameComponent("app", ""), new NameComponent("service", "kind")),
        name.parentComponents());
  }

  @Test
  void escapesSlashDotAndBackslashRoundTrip() {
    NamingName name =
        new NamingName(
            List.of(
                new NameComponent("a/b", "c.d"),
                new NameComponent("back\\slash", ""),
                new NameComponent("", "kind")));

    String stringified = name.stringified();

    assertEquals("a\\/b.c\\.d/back\\\\slash/.kind", stringified);
    assertEquals(name, NamingName.parse(stringified));
  }

  @Test
  void returnedComponentsAreImmutable() {
    NamingName name = NamingName.parse("a/b");

    assertThrows(
        UnsupportedOperationException.class, () -> name.components().add(NameComponent.id("c")));
    assertThrows(
        UnsupportedOperationException.class,
        () -> name.parentComponents().add(NameComponent.id("c")));
  }

  @Test
  void parsesKindOnlyComponentsAndRejectsNullFactoryInputs() {
    NamingName name = NamingName.parse(".kind/leaf");

    assertEquals(new NameComponent("", "kind"), name.components().get(0));
    assertEquals(".kind/leaf", name.stringified());
    assertThrows(NullPointerException.class, () -> NamingName.parse(null));
    assertThrows(NullPointerException.class, () -> NamingName.of(null));
    assertThrows(
        NullPointerException.class,
        () -> NamingName.of(NameComponent.id("a"), (NameComponent) null));
    assertThrows(NullPointerException.class, () -> NameComponent.id(null));
  }

  @Test
  void rejectsEmptyAndMalformedStringifiedNames() {
    assertInvalid(() -> NamingName.parse(""));
    assertInvalid(() -> NamingName.parse("/a"));
    assertInvalid(() -> NamingName.parse("a/"));
    assertInvalid(() -> NamingName.parse("a//b"));
    assertInvalid(() -> NamingName.parse("."));
    assertInvalid(() -> NamingName.parse("a.b.c"));
    assertInvalid(() -> NamingName.parse("a\\"));
    assertInvalid(() -> NamingName.parse("a\\q"));
    assertInvalid(() -> new NameComponent("", ""));
    assertInvalid(() -> new NamingName(List.of()));
  }

  private static void assertInvalid(ThrowingRunnable runnable) {
    NamingException exception = assertThrows(NamingException.class, runnable::run);
    assertEquals(NamingDiagnosticCodes.INVALID_NAME, exception.code());
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run();
  }
}
