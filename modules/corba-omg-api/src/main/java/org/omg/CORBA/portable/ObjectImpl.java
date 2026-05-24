package org.omg.CORBA.portable;

/** Base class for generated legacy stubs. */
public abstract class ObjectImpl implements org.omg.CORBA.Object {

  private Delegate delegate;

  /** Creates a generated-stub base object. */
  protected ObjectImpl() {}

  /** Returns repository IDs supported by the generated stub. */
  public abstract String[] _ids();

  /** Returns the delegate. */
  public Delegate _get_delegate() {
    if (delegate == null) {
      throw new org.omg.CORBA.BAD_INV_ORDER("Delegate has not been set");
    }
    return delegate;
  }

  /** Sets the delegate. */
  public void _set_delegate(Delegate delegate) {
    this.delegate = java.util.Objects.requireNonNull(delegate, "delegate");
  }

  /** Creates a request stream. */
  public OutputStream _request(String operation, boolean responseExpected) {
    return _get_delegate().request(this, operation, responseExpected);
  }

  /** Invokes a request. */
  public InputStream _invoke(OutputStream output) throws ApplicationException, RemarshalException {
    return _get_delegate().invoke(this, output);
  }

  /** Releases a reply stream. */
  public void _releaseReply(InputStream input) {
    _get_delegate().releaseReply(this, input);
  }

  @Override
  public boolean _is_a(String repositoryIdentifier) {
    return _get_delegate().is_a(this, repositoryIdentifier);
  }

  @Override
  public boolean _is_equivalent(org.omg.CORBA.Object other) {
    return _get_delegate().is_equivalent(this, other);
  }

  @Override
  public boolean _non_existent() {
    return _get_delegate().non_existent(this);
  }

  @Override
  public int _hash(int maximum) {
    return _get_delegate().hash(this, maximum);
  }

  @Override
  public org.omg.CORBA.Request _request(String operation) {
    throw unsupported();
  }

  @Override
  public org.omg.CORBA.Request _create_request(
      org.omg.CORBA.Context context,
      String operation,
      org.omg.CORBA.NVList arguments,
      org.omg.CORBA.NamedValue result) {
    throw unsupported();
  }

  @Override
  public org.omg.CORBA.Request _create_request(
      org.omg.CORBA.Context context,
      String operation,
      org.omg.CORBA.NVList arguments,
      org.omg.CORBA.NamedValue result,
      org.omg.CORBA.ExceptionList exceptions,
      org.omg.CORBA.ContextList contexts) {
    throw unsupported();
  }

  @Override
  public org.omg.CORBA.Policy _get_policy(int policyType) {
    throw unsupported();
  }

  @Override
  public org.omg.CORBA.DomainManager[] _get_domain_managers() {
    return new org.omg.CORBA.DomainManager[0];
  }

  @Override
  public org.omg.CORBA.Object _set_policy_override(
      org.omg.CORBA.Policy[] policies, org.omg.CORBA.SetOverrideType setAdd) {
    throw unsupported();
  }

  private static org.omg.CORBA.NO_IMPLEMENT unsupported() {
    return new org.omg.CORBA.NO_IMPLEMENT("Dynamic object operation is not implemented");
  }
}
