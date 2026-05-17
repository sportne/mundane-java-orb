package io.github.mundanej.mjo.idl.java.mapping;

/** IDL-to-Java source mapping modes supported by the first generation slice. */
public enum JavaMappingMode {
  /** Compile-safe legacy-oriented naming without helpers, holders, stubs, or POA classes. */
  LEGACY_COMPATIBILITY,
  /** Compile-safe modern naming, distinct from the legacy package namespace. */
  MODERN
}
