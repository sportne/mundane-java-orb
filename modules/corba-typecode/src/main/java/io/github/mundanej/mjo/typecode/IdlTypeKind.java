package io.github.mundanej.mjo.typecode;

/** IDL type categories represented by generated static descriptors. */
public enum IdlTypeKind {
  /** The IDL `void` operation return type. */
  VOID,
  /** Built-in scalar or string-like IDL type. */
  PRIMITIVE,
  /** IDL interface declaration. */
  INTERFACE,
  /** IDL struct declaration. */
  STRUCT,
  /** IDL enum declaration. */
  ENUM,
  /** IDL user exception declaration. */
  EXCEPTION
}
