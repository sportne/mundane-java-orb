package org.omg.PortableInterceptor;

import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

/** ORB initialization information compatibility surface. */
public interface ORBInitInfo {

  /** Returns the ORB init arguments. */
  String[] arguments();

  /** Returns the ORB ID. */
  String orb_id();

  /** Registers a client request interceptor. */
  void add_client_request_interceptor(ClientRequestInterceptor interceptor) throws DuplicateName;

  /** Registers a server request interceptor. */
  void add_server_request_interceptor(ServerRequestInterceptor interceptor) throws DuplicateName;
}
