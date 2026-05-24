package org.omg.CORBA;

/** Local-only object implementation whose operations are unsupported by default. */
public class LocalObject implements Object {

  /** Creates a local compatibility object. */
  public LocalObject() {}

  @Override
  public boolean _is_a(String repositoryIdentifier) {
    throw unsupported();
  }

  @Override
  public boolean _is_equivalent(Object other) {
    return this == other;
  }

  @Override
  public boolean _non_existent() {
    return false;
  }

  @Override
  public int _hash(int maximum) {
    return maximum == 0 ? 0 : java.lang.Math.floorMod(System.identityHashCode(this), maximum);
  }

  @Override
  public Request _request(String operation) {
    throw unsupported();
  }

  @Override
  public Request _create_request(
      Context context, String operation, NVList arguments, NamedValue result) {
    throw unsupported();
  }

  @Override
  public Request _create_request(
      Context context,
      String operation,
      NVList arguments,
      NamedValue result,
      ExceptionList exceptions,
      ContextList contexts) {
    throw unsupported();
  }

  @Override
  public Policy _get_policy(int policyType) {
    throw unsupported();
  }

  @Override
  public DomainManager[] _get_domain_managers() {
    return new DomainManager[0];
  }

  @Override
  public Object _set_policy_override(Policy[] policies, SetOverrideType setAdd) {
    throw unsupported();
  }

  private static NO_IMPLEMENT unsupported() {
    return new NO_IMPLEMENT("CORBA local object behavior is not implemented");
  }
}
