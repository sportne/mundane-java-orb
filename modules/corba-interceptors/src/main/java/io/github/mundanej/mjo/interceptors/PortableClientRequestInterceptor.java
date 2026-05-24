package io.github.mundanej.mjo.interceptors;

/** Local Portable Interceptor client request hook. */
public interface PortableClientRequestInterceptor {

  /** Returns the unique interceptor name within the client interceptor registry. */
  String name();

  /** Called before a GIOP request is sent. */
  default void sendRequest(ClientRequestContext context) {}

  /** Called after a normal GIOP reply is received and decoded. */
  default void receiveReply(ClientRequestContext context) {}

  /** Called after a user or system exception reply is received. */
  default void receiveException(ClientRequestContext context) {}
}
