package org.omg.PortableInterceptor;

/** Base Portable Interceptor compatibility surface. */
public interface Interceptor {

  /** Returns the interceptor name. */
  String name();

  /** Destroys the interceptor. */
  void destroy();
}
