package io.github.mundanej.mjo.idl.semantics;

/** Semantic IDL symbol categories emitted by the minimal semantic analyzer. */
public enum IdlSymbolKind {
  /** IDL module scope. */
  MODULE,
  /** IDL interface type. */
  INTERFACE,
  /** IDL operation member. */
  OPERATION,
  /** IDL attribute member. */
  ATTRIBUTE,
  /** IDL struct type. */
  STRUCT,
  /** IDL typedef alias. */
  TYPEDEF,
  /** IDL union type. */
  UNION,
  /** IDL struct or exception field. */
  FIELD,
  /** IDL enum type. */
  ENUM,
  /** IDL enum value. */
  ENUMERATOR,
  /** IDL exception type. */
  EXCEPTION,
  /** IDL constant. */
  CONSTANT,
  /** IDL operation parameter. */
  PARAMETER,
  /** IDL native type declaration. */
  NATIVE,
  /** IDL valuetype declaration. */
  VALUETYPE,
  /** IDL value box declaration. */
  VALUE_BOX,
  /** IDL valuetype factory. */
  VALUE_FACTORY,
  /** IDL valuetype state member. */
  VALUE_FIELD
}
