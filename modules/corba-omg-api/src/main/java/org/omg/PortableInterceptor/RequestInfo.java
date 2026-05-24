package org.omg.PortableInterceptor;

/** Shared request information compatibility surface. */
public interface RequestInfo {

  /** Returns the request ID. */
  int request_id();

  /** Returns the operation name. */
  String operation();

  /** Returns request arguments. */
  org.omg.DynamicAny.NameValuePair[] arguments();

  /** Returns response expected flag. */
  boolean response_expected();
}
