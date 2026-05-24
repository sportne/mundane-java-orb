package org.omg.PortableInterceptor;

/** Interceptor request-forwarding exception. */
@SuppressWarnings("serial")
public final class ForwardRequest extends org.omg.CORBA.UserException {

  private static final long serialVersionUID = 1L;

  /** Forward target. */
  public org.omg.CORBA.Object forward;

  /** Creates an empty forward request. */
  public ForwardRequest() {}

  /** Creates a forward request. */
  public ForwardRequest(org.omg.CORBA.Object forward) {
    this.forward = forward;
  }
}
