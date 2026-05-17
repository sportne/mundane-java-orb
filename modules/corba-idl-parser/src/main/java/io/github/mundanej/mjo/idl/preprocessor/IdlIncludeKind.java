package io.github.mundanej.mjo.idl.preprocessor;

/** IDL include spelling used by a preprocessor include directive. */
public enum IdlIncludeKind {
  /** Quoted include such as {@code #include "types.idl"}. */
  QUOTED,
  /** System include such as {@code #include <omg/CORBA.idl>}. */
  SYSTEM
}
