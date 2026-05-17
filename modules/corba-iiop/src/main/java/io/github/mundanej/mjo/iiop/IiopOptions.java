package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.giop.GiopLimits;
import java.time.Duration;
import java.util.Objects;

/**
 * Bounded local TCP options for the first IIOP transport slice.
 *
 * @param connectTimeout TCP connect timeout
 * @param readTimeout socket read timeout
 * @param serverBacklog server socket backlog
 * @param maxOpenConnections maximum concurrent accepted connections
 * @param giopLimits GIOP frame and service-context limits
 */
public record IiopOptions(
    Duration connectTimeout,
    Duration readTimeout,
    int serverBacklog,
    int maxOpenConnections,
    GiopLimits giopLimits) {

  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(2);
  private static final int DEFAULT_SERVER_BACKLOG = 16;
  private static final int DEFAULT_MAX_OPEN_CONNECTIONS = 16;

  /** Creates validated TCP options. */
  public IiopOptions {
    connectTimeout = requireNonNegative(Objects.requireNonNull(connectTimeout, "connectTimeout"));
    readTimeout = requireNonNegative(Objects.requireNonNull(readTimeout, "readTimeout"));
    if (serverBacklog <= 0) {
      throw new IiopException(
          IiopDiagnosticCodes.INVALID_CONFIGURATION, "serverBacklog must be positive");
    }
    if (maxOpenConnections <= 0) {
      throw new IiopException(
          IiopDiagnosticCodes.INVALID_CONFIGURATION, "maxOpenConnections must be positive");
    }
    Objects.requireNonNull(giopLimits, "giopLimits");
    requireIntMillis(connectTimeout, "connectTimeout");
    requireIntMillis(readTimeout, "readTimeout");
  }

  /** Returns conservative defaults for local loopback tests. */
  public static IiopOptions defaults() {
    return new IiopOptions(
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_READ_TIMEOUT,
        DEFAULT_SERVER_BACKLOG,
        DEFAULT_MAX_OPEN_CONNECTIONS,
        GiopLimits.defaults());
  }

  int connectTimeoutMillis() {
    return requireIntMillis(connectTimeout, "connectTimeout");
  }

  int readTimeoutMillis() {
    return requireIntMillis(readTimeout, "readTimeout");
  }

  private static Duration requireNonNegative(Duration value) {
    if (value.isNegative()) {
      throw new IiopException(
          IiopDiagnosticCodes.INVALID_CONFIGURATION, "timeout durations must not be negative");
    }
    return value;
  }

  private static int requireIntMillis(Duration value, String name) {
    long millis = value.toMillis();
    if (millis > Integer.MAX_VALUE) {
      throw new IiopException(
          IiopDiagnosticCodes.INVALID_CONFIGURATION, name + " must fit in socket timeout range");
    }
    return Math.toIntExact(millis);
  }
}
