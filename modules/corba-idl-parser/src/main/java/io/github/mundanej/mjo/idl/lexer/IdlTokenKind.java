package io.github.mundanej.mjo.idl.lexer;

/** Stable IDL lexical token categories. */
public enum IdlTokenKind {
  /** Plain IDL identifier. */
  IDENTIFIER,
  /** Identifier escaped with a leading underscore to avoid keyword checking. */
  ESCAPED_IDENTIFIER,
  /** Exact-case IDL keyword. */
  KEYWORD,
  /** Integer literal. */
  INTEGER_LITERAL,
  /** Floating-point literal. */
  FLOATING_POINT_LITERAL,
  /** Fixed-point literal. */
  FIXED_POINT_LITERAL,
  /** Character literal. */
  CHARACTER_LITERAL,
  /** Wide character literal. */
  WIDE_CHARACTER_LITERAL,
  /** String literal. */
  STRING_LITERAL,
  /** Wide string literal. */
  WIDE_STRING_LITERAL,
  /** Semicolon token. */
  SEMICOLON,
  /** Left brace token. */
  LEFT_BRACE,
  /** Right brace token. */
  RIGHT_BRACE,
  /** Colon token. */
  COLON,
  /** Double-colon token. */
  DOUBLE_COLON,
  /** Comma token. */
  COMMA,
  /** Equals token. */
  EQUALS,
  /** Plus token. */
  PLUS,
  /** Minus token. */
  MINUS,
  /** Left parenthesis token. */
  LEFT_PAREN,
  /** Right parenthesis token. */
  RIGHT_PAREN,
  /** Less-than token. */
  LESS_THAN,
  /** Shift-left token. */
  SHIFT_LEFT,
  /** Greater-than token. */
  GREATER_THAN,
  /** Shift-right token. */
  SHIFT_RIGHT,
  /** Left bracket token. */
  LEFT_BRACKET,
  /** Right bracket token. */
  RIGHT_BRACKET,
  /** Backslash token. */
  BACKSLASH,
  /** Vertical bar token. */
  VERTICAL_BAR,
  /** Logical-or token. */
  LOGICAL_OR,
  /** Caret token. */
  CARET,
  /** Ampersand token. */
  AMPERSAND,
  /** Logical-and token. */
  LOGICAL_AND,
  /** Asterisk token. */
  ASTERISK,
  /** Slash token. */
  SLASH,
  /** Percent token. */
  PERCENT,
  /** Tilde token. */
  TILDE,
  /** At-sign token. */
  AT_SIGN,
  /** Hash token. */
  HASH,
  /** Double-hash token. */
  DOUBLE_HASH,
  /** Exclamation token. */
  EXCLAMATION,
  /** Invalid token emitted for recovery. */
  INVALID_TOKEN,
  /** End of source marker. */
  END_OF_FILE
}
