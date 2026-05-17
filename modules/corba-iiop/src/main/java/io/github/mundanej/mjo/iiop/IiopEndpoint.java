package io.github.mundanej.mjo.iiop;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Objects;

/**
 * TCP host and port used by the local IIOP transport slice.
 *
 * @param host socket host name or address
 * @param port unsigned TCP port; {@code 0} requests an ephemeral server bind
 */
public record IiopEndpoint(String host, int port) {

  /** Creates a validated endpoint. */
  public IiopEndpoint {
    host = requireHost(host);
    requireUnsignedShort(port, "port");
  }

  /** Returns a loopback endpoint for the supplied port. */
  public static IiopEndpoint loopback(int port) {
    return new IiopEndpoint(InetAddress.getLoopbackAddress().getHostAddress(), port);
  }

  static IiopEndpoint fromBoundSocket(ServerSocket socket) {
    return new IiopEndpoint(socket.getInetAddress().getHostAddress(), socket.getLocalPort());
  }

  static int requireUnsignedShort(int value, String name) {
    if (value < 0 || value > 0xFFFF) {
      throw new IiopException(
          IiopDiagnosticCodes.INVALID_CONFIGURATION, name + " must fit in unsigned short");
    }
    return value;
  }

  private static String requireHost(String value) {
    Objects.requireNonNull(value, "host");
    if (value.isBlank()) {
      throw new IiopException(IiopDiagnosticCodes.INVALID_CONFIGURATION, "host is required");
    }
    return value;
  }
}
