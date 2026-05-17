package io.github.mundanej.mjo.idlj;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/** Deterministic text formatter for idlj diagnostics. */
public final class IdljDiagnosticFormatter {

  /** Formats one diagnostic in source-order-friendly command-line form. */
  public String format(Diagnostic diagnostic) {
    Objects.requireNonNull(diagnostic, "diagnostic");
    String body =
        diagnostic.severity().name()
            + " "
            + diagnostic.code().value()
            + ": "
            + diagnostic.message();
    return diagnostic.span().map(span -> prefix(span) + body).orElse(body);
  }

  private static String prefix(SourceSpan span) {
    return span.start().sourceName()
        + ":"
        + span.start().line()
        + ":"
        + span.start().column()
        + ": ";
  }
}
