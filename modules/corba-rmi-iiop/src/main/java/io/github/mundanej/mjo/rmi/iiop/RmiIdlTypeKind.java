package io.github.mundanej.mjo.rmi.iiop;

/** IDL type-reference categories produced by the G7-020 Java-to-IDL model. */
public enum RmiIdlTypeKind {
  /** IDL void return type. */
  VOID,

  /** IDL primitive or string-like built-in type. */
  BUILTIN,

  /** IDL scoped reference to a Java value or remote interface type. */
  DECLARED_VALUE,

  /** IDL sequence type reference derived from a Java array type. */
  SEQUENCE
}
