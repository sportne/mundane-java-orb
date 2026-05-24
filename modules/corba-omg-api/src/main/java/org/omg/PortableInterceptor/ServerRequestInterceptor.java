package org.omg.PortableInterceptor;

/** Server request interceptor compatibility surface. */
public interface ServerRequestInterceptor extends Interceptor {

  /** Called when service contexts are available. */
  void receive_request_service_contexts(ServerRequestInfo requestInfo) throws ForwardRequest;

  /** Called when the request body is available. */
  void receive_request(ServerRequestInfo requestInfo) throws ForwardRequest;

  /** Called before a normal reply is sent. */
  void send_reply(ServerRequestInfo requestInfo);

  /** Called before an exception reply is sent. */
  void send_exception(ServerRequestInfo requestInfo) throws ForwardRequest;

  /** Called before another-location reply is sent. */
  void send_other(ServerRequestInfo requestInfo) throws ForwardRequest;
}
