package io.github.mundanej.mjo.iiop;

import java.util.List;
import java.util.Objects;
import javax.net.ssl.SSLContext;

/**
 * Endpoint-local TLS configuration for IIOP sockets.
 *
 * @param mode plain TCP, server-authenticated TLS, or mutual TLS
 * @param sslContext caller-provided SSL context for TLS modes
 * @param enabledProtocols optional enabled protocol names
 * @param enabledCipherSuites optional enabled cipher-suite names
 */
public record IiopTlsOptions(
    Mode mode,
    SSLContext sslContext,
    List<String> enabledProtocols,
    List<String> enabledCipherSuites) {

  /** TLS operating mode for one endpoint. */
  public enum Mode {
    /** Use plain TCP sockets. */
    DISABLED,

    /** Use TLS with server authentication. */
    TLS,

    /** Use TLS and require client certificate authentication on servers. */
    MTLS
  }

  /** Creates validated endpoint-local TLS options. */
  public IiopTlsOptions {
    Objects.requireNonNull(mode, "mode");
    enabledProtocols = validateNames(enabledProtocols, "enabledProtocols");
    enabledCipherSuites = validateNames(enabledCipherSuites, "enabledCipherSuites");
    if (mode == Mode.DISABLED) {
      if (sslContext != null) {
        throw new IiopException(
            IiopDiagnosticCodes.INVALID_CONFIGURATION,
            "disabled TLS options must not carry an SSLContext");
      }
    } else {
      Objects.requireNonNull(sslContext, "sslContext");
    }
  }

  /** Returns disabled TLS options for plain TCP. */
  public static IiopTlsOptions disabled() {
    return new IiopTlsOptions(Mode.DISABLED, null, List.of(), List.of());
  }

  /** Returns TLS options with the supplied endpoint-local context. */
  public static IiopTlsOptions tls(SSLContext sslContext) {
    return new IiopTlsOptions(Mode.TLS, sslContext, List.of(), List.of());
  }

  /** Returns mutual TLS options with the supplied endpoint-local context. */
  public static IiopTlsOptions mutualTls(SSLContext sslContext) {
    return new IiopTlsOptions(Mode.MTLS, sslContext, List.of(), List.of());
  }

  /** Returns a defensive copy of enabled protocol names. */
  @Override
  public List<String> enabledProtocols() {
    return List.copyOf(enabledProtocols);
  }

  /** Returns a defensive copy of enabled cipher-suite names. */
  @Override
  public List<String> enabledCipherSuites() {
    return List.copyOf(enabledCipherSuites);
  }

  boolean enabled() {
    return mode != Mode.DISABLED;
  }

  boolean mutualTls() {
    return mode == Mode.MTLS;
  }

  private static List<String> validateNames(List<String> values, String name) {
    List<String> copy = List.copyOf(Objects.requireNonNull(values, name));
    for (String value : copy) {
      if (value.isBlank()) {
        throw new IiopException(
            IiopDiagnosticCodes.INVALID_CONFIGURATION, name + " must not contain blank names");
      }
    }
    return copy;
  }
}
