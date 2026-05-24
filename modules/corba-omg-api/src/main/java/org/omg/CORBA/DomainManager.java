package org.omg.CORBA;

/** CORBA domain manager compatibility surface. */
public interface DomainManager extends Object {

  /** Returns the domain policy for a policy type. */
  Policy get_domain_policy(int policyType);
}
