package io.github.mundanej.mjo.idl.preprocessor;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/**
 * Include lookup request passed to an {@link IdlIncludeResolver}.
 *
 * @param includeName include name exactly as spelled between quotes or angle brackets
 * @param kind quoted or system include spelling
 * @param requestingSourceName source that requested the include
 * @param span source span for the include name or directive
 */
public record IdlIncludeRequest(
    String includeName, IdlIncludeKind kind, String requestingSourceName, SourceSpan span) {

  /** Creates a validated include request. */
  public IdlIncludeRequest {
    includeName = requireNonBlank(includeName, "includeName");
    Objects.requireNonNull(kind, "kind");
    requestingSourceName = requireNonBlank(requestingSourceName, "requestingSourceName");
    Objects.requireNonNull(span, "span");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
