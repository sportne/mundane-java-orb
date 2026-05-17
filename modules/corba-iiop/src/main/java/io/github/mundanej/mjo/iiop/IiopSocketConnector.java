package io.github.mundanej.mjo.iiop;

import java.io.IOException;
import java.net.Socket;

@FunctionalInterface
interface IiopSocketConnector {

  Socket connect(IiopEndpoint endpoint, IiopOptions options) throws IOException;
}
