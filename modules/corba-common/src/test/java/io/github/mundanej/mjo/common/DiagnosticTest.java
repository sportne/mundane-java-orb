package io.github.mundanej.mjo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Diagnostic}. */
@Tag("unit")
final class DiagnosticTest {

  @Test
  void createsDiagnosticWithoutSourceSpan() {
    Diagnostic diagnostic =
        Diagnostic.withoutSpan(
            new DiagnosticCode("IDL-0001"), DiagnosticSeverity.ERROR, "Invalid token");

    assertEquals(new DiagnosticCode("IDL-0001"), diagnostic.code());
    assertEquals(DiagnosticSeverity.ERROR, diagnostic.severity());
    assertEquals("Invalid token", diagnostic.message());
    assertFalse(diagnostic.span().isPresent());
  }

  @Test
  void createsDiagnosticWithSourceSpan() {
    SourcePosition start = new SourcePosition("hello.idl", 1, 1, 0);
    SourcePosition end = new SourcePosition("hello.idl", 1, 2, 1);
    SourceSpan span = new SourceSpan(start, end);

    Diagnostic diagnostic =
        Diagnostic.withSpan(
            new DiagnosticCode("IDL-0002"), DiagnosticSeverity.WARNING, "Suspicious token", span);

    assertEquals(Optional.of(span), diagnostic.span());
  }

  @Test
  void rejectsBlankMessage() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Diagnostic.withoutSpan(new DiagnosticCode("IDL-0001"), DiagnosticSeverity.ERROR, " "));
  }

  @Test
  void rejectsNullDiagnosticFields() {
    DiagnosticCode code = new DiagnosticCode("IDL-0001");

    assertThrows(
        NullPointerException.class,
        () -> Diagnostic.withoutSpan(null, DiagnosticSeverity.ERROR, "Invalid token"));
    assertThrows(
        NullPointerException.class, () -> Diagnostic.withoutSpan(code, null, "Invalid token"));
    assertThrows(
        NullPointerException.class,
        () -> Diagnostic.withoutSpan(code, DiagnosticSeverity.ERROR, null));
    assertThrows(
        NullPointerException.class,
        () -> new Diagnostic(code, DiagnosticSeverity.ERROR, "Invalid token", null));
    assertThrows(
        NullPointerException.class,
        () -> Diagnostic.withSpan(code, DiagnosticSeverity.ERROR, "Invalid token", null));
  }

  @Test
  void diagnosticSeverityOrderIsStableForReports() {
    assertEquals(
        java.util.List.of(
            DiagnosticSeverity.INFO, DiagnosticSeverity.WARNING, DiagnosticSeverity.ERROR),
        java.util.List.of(DiagnosticSeverity.values()));
  }

  @Test
  void recordsHaveValueEquality() {
    Diagnostic first =
        Diagnostic.withoutSpan(
            new DiagnosticCode("IDL-0001"), DiagnosticSeverity.INFO, "Read source");
    Diagnostic second =
        Diagnostic.withoutSpan(
            new DiagnosticCode("IDL-0001"), DiagnosticSeverity.INFO, "Read source");

    assertEquals(first, second);
    assertTrue(first.span().isEmpty());
  }
}
