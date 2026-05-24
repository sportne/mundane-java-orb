package org.omg.PortableInterceptor;

/** ORB initializer compatibility surface. */
public interface ORBInitializer {

  /** Called before ORB initialization completes. */
  void pre_init(ORBInitInfo info);

  /** Called after ORB initialization completes. */
  void post_init(ORBInitInfo info);
}
