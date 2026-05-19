package io.github.mundanej.mjo.rmi.iiop;

/** Java method shapes relevant to G7-010 eligibility classification. */
public enum RmiJavaOperationKind {
  /** Normal abstract method declaration on a remote interface. */
  ABSTRACT,

  /** Java default method declaration, deferred for later design if ever allowed. */
  DEFAULT,

  /** Static interface method declaration, outside the RMI remote operation set. */
  STATIC,

  /** Private interface method declaration, outside the RMI remote operation set. */
  PRIVATE
}
