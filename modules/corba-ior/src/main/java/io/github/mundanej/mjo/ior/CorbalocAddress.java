package io.github.mundanej.mjo.ior;

import java.util.Objects;
import java.util.Optional;

/** One address entry inside a {@code corbaloc:} object URL. */
public final class CorbalocAddress {

  /** Supported address kinds in the first object URL slice. */
  public enum Kind {
    /** Internet Inter-ORB Protocol address. */
    IIOP,
    /** Resolve-initial-references address marker. */
    RIR,
    /** Future or implementation-specific protocol address. */
    FUTURE
  }

  private final Kind kind;
  private final String protocol;
  private final IiopVersion version;
  private final String host;
  private final int port;
  private final String protocolSpecificAddress;

  private CorbalocAddress(
      Kind kind,
      String protocol,
      IiopVersion version,
      String host,
      int port,
      String protocolSpecificAddress) {
    this.kind = Objects.requireNonNull(kind, "kind");
    this.protocol = IorWire.requireNonBlank(protocol, "protocol");
    this.version = version;
    this.host = host;
    this.port = port;
    this.protocolSpecificAddress = protocolSpecificAddress;
  }

  /** Creates an IIOP corbaloc address. */
  public static CorbalocAddress iiop(IiopVersion version, String host, int port) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(host, "host");
    return new CorbalocAddress(
        Kind.IIOP, "iiop", version, host, IorWire.requireUnsignedShort(port, "IIOP port"), "");
  }

  /** Creates a resolve-initial-references corbaloc address. */
  public static CorbalocAddress rir() {
    return new CorbalocAddress(Kind.RIR, "rir", null, null, -1, "");
  }

  /** Creates a future or implementation-specific corbaloc address. */
  public static CorbalocAddress future(String protocol, String protocolSpecificAddress) {
    return new CorbalocAddress(
        Kind.FUTURE,
        protocol,
        null,
        null,
        -1,
        Objects.requireNonNull(protocolSpecificAddress, "protocolSpecificAddress"));
  }

  /** Returns the address kind. */
  public Kind kind() {
    return kind;
  }

  /** Returns the protocol token. */
  public String protocol() {
    return protocol;
  }

  /** Returns the IIOP version when this is an IIOP address. */
  public Optional<IiopVersion> version() {
    return Optional.ofNullable(version);
  }

  /** Returns the host when this is an IIOP address. */
  public Optional<String> host() {
    return Optional.ofNullable(host);
  }

  /** Returns the port when this is an IIOP address. */
  public Optional<Integer> port() {
    return port < 0 ? Optional.empty() : Optional.of(port);
  }

  /** Returns the address payload when this is a future protocol address. */
  public Optional<String> protocolSpecificAddress() {
    return Optional.ofNullable(protocolSpecificAddress).filter(value -> !value.isEmpty());
  }

  @Override
  public String toString() {
    return switch (kind) {
      case IIOP -> "CorbalocAddress[iiop " + version + "@" + host + ":" + port + "]";
      case RIR -> "CorbalocAddress[rir]";
      case FUTURE -> "CorbalocAddress[" + protocol + ":" + protocolSpecificAddress + "]";
    };
  }
}
