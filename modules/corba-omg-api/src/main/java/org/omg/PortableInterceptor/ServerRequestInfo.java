package org.omg.PortableInterceptor;

/** Server request information compatibility surface. */
public interface ServerRequestInfo extends RequestInfo {

  /** Returns the server object ID. */
  byte[] object_id();

  /** Returns the adapter ID. */
  byte[] adapter_id();
}
