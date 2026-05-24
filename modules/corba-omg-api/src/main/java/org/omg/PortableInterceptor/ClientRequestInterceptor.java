package org.omg.PortableInterceptor;

/** Client request interceptor compatibility surface. */
public interface ClientRequestInterceptor extends Interceptor {

  /** Called before a client request is sent. */
  void send_request(ClientRequestInfo requestInfo) throws ForwardRequest;

  /** Called before a poll. */
  void send_poll(ClientRequestInfo requestInfo);

  /** Called when a normal reply is received. */
  void receive_reply(ClientRequestInfo requestInfo);

  /** Called when an exception reply is received. */
  void receive_exception(ClientRequestInfo requestInfo) throws ForwardRequest;

  /** Called when another location should be used. */
  void receive_other(ClientRequestInfo requestInfo) throws ForwardRequest;
}
