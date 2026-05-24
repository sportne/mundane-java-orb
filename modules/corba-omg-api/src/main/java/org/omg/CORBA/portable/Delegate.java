package org.omg.CORBA.portable;

/** Delegate used by generated object stubs. */
public abstract class Delegate {

  /** Returns whether the object supports a repository ID. */
  public abstract boolean is_a(org.omg.CORBA.Object self, String repositoryIdentifier);

  /** Returns whether two references are equivalent. */
  public abstract boolean is_equivalent(org.omg.CORBA.Object self, org.omg.CORBA.Object other);

  /** Returns whether the target no longer exists. */
  public abstract boolean non_existent(org.omg.CORBA.Object self);

  /** Returns a bounded hash value. */
  public abstract int hash(org.omg.CORBA.Object self, int maximum);

  /** Creates a request output stream. */
  public abstract OutputStream request(
      org.omg.CORBA.Object self, String operation, boolean responseExpected);

  /** Invokes a request and returns the reply stream. */
  public abstract InputStream invoke(org.omg.CORBA.Object self, OutputStream output)
      throws ApplicationException, RemarshalException;

  /** Releases a reply input stream. */
  public abstract void releaseReply(org.omg.CORBA.Object self, InputStream input);
}
