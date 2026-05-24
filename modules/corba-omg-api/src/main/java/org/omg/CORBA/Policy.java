package org.omg.CORBA;

/** CORBA policy compatibility surface. */
public interface Policy {

  /** Returns the policy type ID. */
  int policy_type();

  /** Returns a copy of this policy. */
  Policy copy();

  /** Destroys this policy. */
  void destroy();
}
