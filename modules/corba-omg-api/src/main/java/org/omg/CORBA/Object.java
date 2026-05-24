package org.omg.CORBA;

/** Legacy CORBA object-reference compatibility surface. */
@SuppressWarnings({"JavaLangClash", "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS"})
public interface Object {

  /** Returns whether this object supports the repository ID. */
  boolean _is_a(String repositoryIdentifier);

  /** Returns whether another reference denotes the same object. */
  boolean _is_equivalent(Object other);

  /** Returns whether the target object no longer exists. */
  boolean _non_existent();

  /** Returns a bounded hash value for this reference. */
  int _hash(int maximum);

  /** Creates a dynamic request for the named operation. */
  Request _request(String operation);

  /** Creates a dynamic request with argument and result metadata. */
  Request _create_request(Context context, String operation, NVList arguments, NamedValue result);

  /** Creates a dynamic request with argument, result, exception, and context metadata. */
  Request _create_request(
      Context context,
      String operation,
      NVList arguments,
      NamedValue result,
      ExceptionList exceptions,
      ContextList contexts);

  /** Returns a policy associated with this object reference. */
  Policy _get_policy(int policyType);

  /** Returns domain managers associated with this object reference. */
  DomainManager[] _get_domain_managers();

  /** Returns a reference with the supplied policy overrides. */
  Object _set_policy_override(Policy[] policies, SetOverrideType setAdd);
}
