package io.github.mundanej.mjo.rmi.iiop;

/** Java type-reference shapes recognized by the G7-010 eligibility model. */
public enum RmiJavaTypeKind {
  /** Java void pseudo-type, valid only as an operation return. */
  VOID,

  /** Java primitive type name. */
  PRIMITIVE,

  /** Java declared type binary name. */
  DECLARED,

  /** Java declared remote object reference binary name. */
  REMOTE,

  /** Java array type shape, deferred to G7-020. */
  ARRAY,

  /** Java generic type shape, deferred to G7-020. */
  GENERIC,

  /** Java wildcard type shape, outside the approved RMI remote operation set. */
  WILDCARD
}
