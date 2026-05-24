package org.omg.CORBA.portable;

/** Local servant wrapper returned by generated stubs. */
@SuppressWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
public final class ServantObject {

  /** Local servant object. */
  public java.lang.Object servant;

  /** Creates an empty servant object. */
  public ServantObject() {}

  /** Creates a servant object wrapper. */
  public ServantObject(java.lang.Object servant) {
    this.servant = servant;
  }

  @Override
  public String toString() {
    return "ServantObject[servant=" + servant + ']';
  }
}
