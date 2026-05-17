package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.giop.GiopMessage;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Objects;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

/** Reusable single-connection IIOP client for local GIOP request/reply exchange. */
public final class IiopClient implements AutoCloseable {

  private final IiopEndpoint endpoint;
  private final IiopOptions options;
  private final Socket socket;
  private boolean closed;

  private IiopClient(IiopEndpoint endpoint, IiopOptions options, Socket socket) {
    this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
    this.options = Objects.requireNonNull(options, "options");
    this.socket = Objects.requireNonNull(socket, "socket");
  }

  /** Connects a client socket to the supplied endpoint. */
  public static IiopClient connect(IiopEndpoint endpoint, IiopOptions options) {
    return connect(endpoint, options, IiopClient::connectSocket);
  }

  static IiopClient connect(
      IiopEndpoint endpoint, IiopOptions options, IiopSocketConnector socketConnector) {
    Objects.requireNonNull(endpoint, "endpoint");
    Objects.requireNonNull(options, "options");
    Objects.requireNonNull(socketConnector, "socketConnector");
    try {
      Socket socket = socketConnector.connect(endpoint, options);
      socket.setSoTimeout(options.readTimeoutMillis());
      return new IiopClient(endpoint, options, socket);
    } catch (SocketTimeoutException exception) {
      throw new IiopException(
          IiopDiagnosticCodes.CONNECT_TIMEOUT,
          "Timed out connecting to " + endpoint.host() + ":" + endpoint.port(),
          exception);
    } catch (SSLException exception) {
      throw new IiopException(
          IiopDiagnosticCodes.TLS_HANDSHAKE_FAILURE,
          "TLS handshake failed for " + endpoint.host() + ":" + endpoint.port(),
          exception);
    } catch (IOException exception) {
      throw new IiopException(
          IiopDiagnosticCodes.CONNECTION_FAILURE,
          "Could not connect to " + endpoint.host() + ":" + endpoint.port(),
          exception);
    }
  }

  /** Sends one request and waits for its correlated reply. */
  public synchronized GiopReply invoke(GiopRequest request) {
    Objects.requireNonNull(request, "request");
    requireOpen();
    try {
      IiopFrameCodec.writeMessage(socket.getOutputStream(), request, options.giopLimits());
      GiopMessage message =
          IiopFrameCodec.readMessage(socket.getInputStream(), options.giopLimits());
      if (!(message instanceof GiopReply reply)) {
        close();
        throw new IiopException(
            IiopDiagnosticCodes.UNSUPPORTED_MESSAGE,
            "IIOP client expected a GIOP Reply but received " + message.header().messageType());
      }
      if (reply.requestId() != request.requestId()) {
        close();
        throw new IiopException(
            IiopDiagnosticCodes.CORRELATION_FAILURE,
            "Reply request id "
                + reply.requestId()
                + " did not match request id "
                + request.requestId());
      }
      return reply;
    } catch (IiopException exception) {
      close();
      throw exception;
    } catch (SSLException exception) {
      close();
      throw new IiopException(
          IiopDiagnosticCodes.TLS_HANDSHAKE_FAILURE, "TLS exchange failed", exception);
    } catch (IOException exception) {
      close();
      if (options.tlsOptions().enabled()) {
        throw new IiopException(
            IiopDiagnosticCodes.TLS_HANDSHAKE_FAILURE, "TLS exchange failed", exception);
      }
      throw new IiopException(
          IiopDiagnosticCodes.CONNECTION_FAILURE, "IIOP client exchange failed", exception);
    }
  }

  /** Returns the remote endpoint configured for this client. */
  public IiopEndpoint endpoint() {
    return endpoint;
  }

  /** Closes the client socket. */
  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      socket.close();
    } catch (IOException ignored) {
      // Close is best-effort and idempotent.
    }
  }

  private void requireOpen() {
    if (closed || socket.isClosed()) {
      throw new IiopException(IiopDiagnosticCodes.LIFECYCLE, "IIOP client is closed");
    }
  }

  private static Socket connectSocket(IiopEndpoint endpoint, IiopOptions options)
      throws IOException {
    Socket socket = createSocket(options);
    try {
      socket.connect(
          new InetSocketAddress(endpoint.host(), endpoint.port()), options.connectTimeoutMillis());
      socket.setSoTimeout(options.readTimeoutMillis());
      if (socket instanceof SSLSocket sslSocket) {
        sslSocket.startHandshake();
      }
      return socket;
    } catch (IOException exception) {
      closeSocket(socket);
      throw exception;
    }
  }

  private static Socket createSocket(IiopOptions options) throws IOException {
    if (!options.tlsOptions().enabled()) {
      return new Socket();
    }
    SSLSocket socket =
        (SSLSocket) options.tlsOptions().sslContext().getSocketFactory().createSocket();
    socket.setUseClientMode(true);
    configureTls(socket, options.tlsOptions());
    return socket;
  }

  private static void configureTls(SSLSocket socket, IiopTlsOptions tlsOptions) {
    if (!tlsOptions.enabledProtocols().isEmpty()) {
      socket.setEnabledProtocols(tlsOptions.enabledProtocols().toArray(String[]::new));
    }
    if (!tlsOptions.enabledCipherSuites().isEmpty()) {
      socket.setEnabledCipherSuites(tlsOptions.enabledCipherSuites().toArray(String[]::new));
    }
  }

  private static void closeSocket(Socket socket) {
    try {
      socket.close();
    } catch (IOException ignored) {
      // Close is best-effort during failed connection setup.
    }
  }
}
