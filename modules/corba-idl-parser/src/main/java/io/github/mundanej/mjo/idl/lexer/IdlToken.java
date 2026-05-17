package io.github.mundanej.mjo.idl.lexer;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;
import java.util.Optional;

/**
 * One IDL lexical token.
 *
 * @param kind token kind
 * @param lexeme exact source text for the token, or empty text for EOF
 * @param span source span for the token
 */
public record IdlToken(IdlTokenKind kind, String lexeme, SourceSpan span) {

  /** Creates a validated token. */
  public IdlToken {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(lexeme, "lexeme");
    Objects.requireNonNull(span, "span");
    if (kind == IdlTokenKind.END_OF_FILE && !lexeme.isEmpty()) {
      throw new IllegalArgumentException("EOF token lexeme must be empty");
    }
    if (kind != IdlTokenKind.END_OF_FILE && lexeme.isEmpty()) {
      throw new IllegalArgumentException("Non-EOF token lexeme must not be empty");
    }
    if (kind == IdlTokenKind.ESCAPED_IDENTIFIER
        && (lexeme.length() < 2 || lexeme.charAt(0) != '_')) {
      throw new IllegalArgumentException("Escaped identifier token must start with underscore");
    }
  }

  /** Returns normalized identifier text for identifier tokens. */
  public Optional<String> identifierText() {
    return switch (kind) {
      case IDENTIFIER -> Optional.of(lexeme);
      case ESCAPED_IDENTIFIER -> Optional.of(lexeme.substring(1));
      default -> Optional.empty();
    };
  }
}
