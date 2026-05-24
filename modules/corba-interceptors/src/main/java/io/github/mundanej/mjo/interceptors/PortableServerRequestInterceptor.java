package io.github.mundanej.mjo.interceptors;

/** Local Portable Interceptor server request hook. */
public interface PortableServerRequestInterceptor {

  /** Returns the unique interceptor name within the server interceptor registry. */
  String name();

  /** Called after request service contexts are available. */
  default void receiveRequestServiceContexts(ServerRequestContext context) {}

  /** Called after a request is mapped to a local object and operation. */
  default void receiveRequest(ServerRequestContext context) {}

  /** Called before a normal reply is sent. */
  default void sendReply(ServerRequestContext context) {}

  /** Called before a user or system exception reply is sent. */
  default void sendException(ServerRequestContext context) {}
}
