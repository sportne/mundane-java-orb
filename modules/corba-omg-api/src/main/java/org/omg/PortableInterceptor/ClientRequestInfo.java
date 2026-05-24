package org.omg.PortableInterceptor;

/** Client request information compatibility surface. */
public interface ClientRequestInfo extends RequestInfo {

  /** Returns the target object. */
  org.omg.CORBA.Object target();

  /** Returns the effective target object. */
  org.omg.CORBA.Object effective_target();
}
