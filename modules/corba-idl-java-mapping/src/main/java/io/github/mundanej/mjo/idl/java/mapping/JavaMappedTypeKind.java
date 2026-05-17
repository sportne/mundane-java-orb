package io.github.mundanej.mjo.idl.java.mapping;

/** Generated Java type categories in the minimal source-generation slice. */
public enum JavaMappedTypeKind {
  /** IDL interface mapped to a Java interface. */
  INTERFACE,
  /** IDL struct mapped to a final Java value class. */
  STRUCT,
  /** IDL enum mapped to a Java enum. */
  ENUM,
  /** IDL exception mapped to a checked Java exception class. */
  EXCEPTION
}
