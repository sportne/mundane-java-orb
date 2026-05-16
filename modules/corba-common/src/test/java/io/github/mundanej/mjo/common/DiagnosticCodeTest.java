package io.github.mundanej.mjo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DiagnosticCode}. */
@Tag("unit")
final class DiagnosticCodeTest {

  @Test
  void acceptsStableAreaNumberForm() {
    DiagnosticCode code = new DiagnosticCode("IDL-0001");

    assertEquals("IDL-0001", code.value());
  }

  @Test
  void rejectsBlankCode() {
    assertThrows(IllegalArgumentException.class, () -> new DiagnosticCode(" "));
  }

  @Test
  void rejectsCodeWithoutFourDigitNumber() {
    assertThrows(IllegalArgumentException.class, () -> new DiagnosticCode("IDL-1"));
  }

  @Test
  void rejectsLowercaseArea() {
    assertThrows(IllegalArgumentException.class, () -> new DiagnosticCode("idl-0001"));
  }
}
