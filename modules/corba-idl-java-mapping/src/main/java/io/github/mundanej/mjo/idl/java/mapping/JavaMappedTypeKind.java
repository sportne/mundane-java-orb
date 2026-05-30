package io.github.mundanej.mjo.idl.java.mapping;

/** Generated Java type categories in the minimal source-generation slice. */
public enum JavaMappedTypeKind {
  /** IDL interface mapped to a Java interface. */
  INTERFACE,
  /** IDL interface forward declaration mapped to a compile-safe Java interface. */
  INTERFACE_FORWARD,
  /** IDL struct mapped to a final Java value class. */
  STRUCT,
  /** IDL enum mapped to a Java enum. */
  ENUM,
  /** IDL exception mapped to a checked Java exception class. */
  EXCEPTION,
  /** IDL typedef alias mapped to legacy helper and holder support. */
  TYPEDEF,
  /** IDL union mapped to a compile-safe Java value class. */
  UNION,
  /** IDL native declaration mapped to a compile-safe opaque Java type. */
  NATIVE,
  /** IDL value box mapped to a compile-safe Java value class. */
  VALUE_BOX,
  /** IDL valuetype mapped to a compile-safe Java value class with operation placeholders. */
  VALUETYPE,
  /** Synthetic holder required for out/inout primitive or anonymous constructed parameters. */
  HOLDER
}
