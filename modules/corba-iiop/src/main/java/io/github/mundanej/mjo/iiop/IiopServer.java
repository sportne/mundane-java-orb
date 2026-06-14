package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.giop.GiopMessage;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** Loopback-capable IIOP TCP server for local GIOP request/reply exchange. */
public final class IiopServer implements AutoCloseable {

  private final IiopEndpoint endpoint;
  private final IiopOptions options;
  private final IiopRequestHandler handler;
  private final ServerSocket serverSocket;
  private final Set<Socket> openSockets = ConcurrentHashMap.newKeySet();
  private final AtomicInteger openConnectionCount = new AtomicInteger();
  private final Thread acceptThread;
  private volatile boolean closed;

  private IiopServer(
      IiopEndpoint endpoint,
      IiopOptions options,
      IiopRequestHandler handler,
      ServerSocket serverSocket) {
    this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
    this.options = Objects.requireNonNull(options, "options");
    this.handler = Objects.requireNonNull(handler, "handler");
    this.serverSocket = Objects.requireNonNull(serverSocket, "serverSocket");
    this.acceptThread = new Thread(this::acceptLoop, "mjo-iiop-accept");
    this.acceptThread.setDaemon(true);
    this.acceptThread.start();
  }

  /** Binds and starts a local IIOP server. */
  public static IiopServer bind(
      IiopEndpoint endpoint, IiopOptions options, IiopRequestHandler handler) {
    Objects.requireNonNull(endpoint, "endpoint");
    Objects.requireNonNull(options, "options");
    Objects.requireNonNull(handler, "handler");
    try {
      ServerSocket serverSocket = createServerSocket(options);
      serverSocket.bind(
          new InetSocketAddress(endpoint.host(), endpoint.port()), options.serverBacklog());
      return new IiopServer(
          IiopEndpoint.fromBoundSocket(serverSocket), options, handler, serverSocket);
    } catch (IOException exception) {
      throw new IiopException(
          IiopDiagnosticCodes.CONNECTION_FAILURE,
          "Could not bind IIOP endpoint " + endpoint.host() + ":" + endpoint.port(),
          exception);
    }
  }

  /** Returns the actual bound endpoint, including the ephemeral port when port 0 was requested. */
  public IiopEndpoint endpoint() {
    return endpoint;
  }

  /** Stops accepting new connections and closes open accepted sockets. */
  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      serverSocket.close();
    } catch (IOException ignored) {
      // Close is best-effort and idempotent.
    }
    if (Thread.currentThread() != acceptThread) {
      try {
        acceptThread.join(1000L);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
    }
    for (Socket socket : openSockets) {
      closeSocket(socket);
    }
  }

  private void acceptLoop() {
    while (!closed) {
      try {
        Socket socket = serverSocket.accept();
        acceptSocket(socket);
      } catch (SocketException exception) {
        if (!closed) {
          close();
        }
      } catch (IOException exception) {
        if (!closed) {
          close();
        }
      }
    }
  }

  private void acceptSocket(Socket socket) throws IOException {
    socket.setSoTimeout(options.readTimeoutMillis());
    int current = openConnectionCount.incrementAndGet();
    if (current > options.maxOpenConnections()) {
      openConnectionCount.decrementAndGet();
      closeSocket(socket);
      return;
    }
    openSockets.add(socket);
    Thread connectionThread = new Thread(() -> handleConnection(socket), "mjo-iiop-connection");
    connectionThread.setDaemon(true);
    connectionThread.start();
  }

  private void handleConnection(Socket socket) {
    try (socket) {
      if (socket instanceof SSLSocket sslSocket) {
        sslSocket.startHandshake();
      }
      while (!closed && !socket.isClosed()) {
        GiopMessage message =
            IiopFrameCodec.readMessage(socket.getInputStream(), options.giopLimits());
        if (!(message instanceof GiopRequest request)) {
          throw new IiopException(
              IiopDiagnosticCodes.UNSUPPORTED_MESSAGE,
              "IIOP server expected a GIOP Request but received " + message.header().messageType());
        }
        GiopReply reply = Objects.requireNonNull(handler.handle(request), "handler reply");
        IiopFrameCodec.writeMessage(socket.getOutputStream(), reply, options.giopLimits());
      }
    } catch (SSLException exception) {
      // TLS negotiation failures close this connection before GIOP exchange.
    } catch (IiopException exception) {
      // The first TCP slice closes the connection on protocol and handler failures.
    } catch (Exception exception) {
      // Handler failures are intentionally surfaced by closing this connection.
    } finally {
      openSockets.remove(socket);
      openConnectionCount.decrementAndGet();
    }
  }

  private static ServerSocket createServerSocket(IiopOptions options) throws IOException {
    if (!options.tlsOptions().enabled()) {
      ServerSocket serverSocket = new ServerSocket();
      serverSocket.setReuseAddress(true);
      return serverSocket;
    }
    SSLServerSocket serverSocket =
        (SSLServerSocket)
            options.tlsOptions().sslContext().getServerSocketFactory().createServerSocket();
    serverSocket.setReuseAddress(true);
    configureTls(serverSocket, options.tlsOptions());
    serverSocket.setNeedClientAuth(options.tlsOptions().mutualTls());
    return serverSocket;
  }

  private static void configureTls(SSLServerSocket serverSocket, IiopTlsOptions tlsOptions) {
    if (!tlsOptions.enabledProtocols().isEmpty()) {
      serverSocket.setEnabledProtocols(tlsOptions.enabledProtocols().toArray(String[]::new));
    }
    if (!tlsOptions.enabledCipherSuites().isEmpty()) {
      serverSocket.setEnabledCipherSuites(tlsOptions.enabledCipherSuites().toArray(String[]::new));
    }
  }

  private static void closeSocket(Socket socket) {
    try {
      socket.close();
    } catch (IOException ignored) {
      // Close is best-effort.
    }
  }
}
