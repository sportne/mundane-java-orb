package io.github.mundanej.mjo.idl.ast;

/** Structural kind for an IDL type reference. */
public enum IdlTypeReferenceKind {
  /** Builtin or user-defined named type. */
  NAMED,
  /** Sequence type, optionally bounded. */
  SEQUENCE,
  /** Bounded string or wstring type. */
  BOUNDED_STRING
}
