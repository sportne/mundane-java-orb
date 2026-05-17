package io.github.mundanej.mjo.ior;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Parsed {@code corbaloc:} object URL. */
public record CorbalocUrl(List<CorbalocAddress> addresses, String keyString, ObjectKey objectKey) {

  private static final String PREFIX = "corbaloc:";
  private static final int DEFAULT_IIOP_PORT = 2809;

  /** Creates a parsed corbaloc URL value. */
  public CorbalocUrl {
    addresses = List.copyOf(Objects.requireNonNull(addresses, "addresses"));
    if (addresses.isEmpty()) {
      throw new IorException(IorDiagnosticCodes.INVALID_OBJECT_URL, "corbaloc requires an address");
    }
    Objects.requireNonNull(keyString, "keyString");
    Objects.requireNonNull(objectKey, "objectKey");
    validateRirExclusivity(addresses);
  }

  /** Parses a corbaloc URL with default bounds. */
  public static CorbalocUrl parse(String value) {
    return parse(value, IorLimits.defaults());
  }

  /** Parses a corbaloc URL with caller-supplied bounds. */
  public static CorbalocUrl parse(String value, IorLimits limits) {
    Objects.requireNonNull(value, "value");
    limits.requireWithin(limits.objectUrlCharacters(), value.length());
    if (!value.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
      throw new IorException(IorDiagnosticCodes.INVALID_OBJECT_URL, "corbaloc scheme is required");
    }
    String body = value.substring(PREFIX.length());
    int slash = body.indexOf('/');
    String addressPart = slash < 0 ? body : body.substring(0, slash);
    String keyPart = slash < 0 ? "" : body.substring(slash + 1);
    if (addressPart.isEmpty()) {
      throw new IorException(IorDiagnosticCodes.INVALID_OBJECT_URL, "corbaloc address is required");
    }
    List<CorbalocAddress> addresses = new ArrayList<>();
    for (String address : splitAddressList(addressPart)) {
      addresses.add(parseAddress(address));
    }
    return new CorbalocUrl(addresses, keyPart, new ObjectKey(decodeUrlOctets(keyPart), limits));
  }

  private static CorbalocAddress parseAddress(String value) {
    if (value.equalsIgnoreCase("rir:")) {
      return CorbalocAddress.rir();
    }
    if (value.startsWith(":")) {
      return parseIiopAddress(value.substring(1));
    }
    if (value.regionMatches(true, 0, "iiop:", 0, "iiop:".length())) {
      return parseIiopAddress(value.substring("iiop:".length()));
    }
    int colon = value.indexOf(':');
    if (colon <= 0) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_OBJECT_URL,
          "corbaloc address has no protocol token: " + value);
    }
    return CorbalocAddress.future(value.substring(0, colon), value.substring(colon + 1));
  }

  private static CorbalocAddress parseIiopAddress(String value) {
    String rest = value;
    IiopVersion version = IiopVersion.V1_0;
    int at = value.indexOf('@');
    if (at >= 0) {
      version = parseVersion(value.substring(0, at));
      rest = value.substring(at + 1);
    }
    HostPort hostPort = parseHostPort(rest);
    return CorbalocAddress.iiop(version, hostPort.host(), hostPort.port());
  }

  private static IiopVersion parseVersion(String value) {
    int dot = value.indexOf('.');
    if (dot <= 0 || dot == value.length() - 1) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_OBJECT_URL, "invalid IIOP version: " + value);
    }
    try {
      return new IiopVersion(
          Integer.parseInt(value.substring(0, dot)), Integer.parseInt(value.substring(dot + 1)));
    } catch (NumberFormatException exception) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_OBJECT_URL, "invalid IIOP version: " + value);
    }
  }

  private static HostPort parseHostPort(String value) {
    if (value.isEmpty()) {
      return new HostPort("", DEFAULT_IIOP_PORT);
    }
    if (value.startsWith("[")) {
      int close = value.indexOf(']');
      if (close < 0) {
        throw new IorException(IorDiagnosticCodes.INVALID_OBJECT_URL, "IPv6 host is missing ']'");
      }
      String host = value.substring(0, close + 1);
      String remainder = value.substring(close + 1);
      return new HostPort(host, parseOptionalPort(remainder));
    }
    int colon = value.lastIndexOf(':');
    if (colon < 0) {
      return new HostPort(value, DEFAULT_IIOP_PORT);
    }
    return new HostPort(value.substring(0, colon), parsePort(value.substring(colon + 1)));
  }

  private static int parseOptionalPort(String value) {
    if (value.isEmpty()) {
      return DEFAULT_IIOP_PORT;
    }
    if (!value.startsWith(":")) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_OBJECT_URL, "unexpected host suffix: " + value);
    }
    return parsePort(value.substring(1));
  }

  private static int parsePort(String value) {
    if (value.isEmpty()) {
      throw new IorException(IorDiagnosticCodes.INVALID_PORT, "port must not be empty");
    }
    try {
      return IorWire.requireUnsignedShort(Integer.parseInt(value), "IIOP port");
    } catch (NumberFormatException exception) {
      throw new IorException(IorDiagnosticCodes.INVALID_PORT, "invalid IIOP port: " + value);
    }
  }

  private static List<String> splitAddressList(String value) {
    List<String> addresses = new ArrayList<>();
    int start = 0;
    int bracketDepth = 0;
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      if (current == '[') {
        bracketDepth++;
      } else if (current == ']') {
        bracketDepth--;
      } else if (current == ',' && bracketDepth == 0) {
        addAddress(addresses, value.substring(start, index));
        start = index + 1;
      }
      if (bracketDepth < 0) {
        throw new IorException(IorDiagnosticCodes.INVALID_OBJECT_URL, "unbalanced IPv6 brackets");
      }
    }
    if (bracketDepth != 0) {
      throw new IorException(IorDiagnosticCodes.INVALID_OBJECT_URL, "unbalanced IPv6 brackets");
    }
    addAddress(addresses, value.substring(start));
    return addresses;
  }

  private static void addAddress(List<String> addresses, String value) {
    if (value.isEmpty()) {
      throw new IorException(IorDiagnosticCodes.INVALID_OBJECT_URL, "empty corbaloc address");
    }
    addresses.add(value);
  }

  static byte[] decodeUrlOctets(String value) {
    ByteArrayOutputStream output = new ByteArrayOutputStream(value.length());
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      if (current == '%') {
        if (index + 2 >= value.length()) {
          throw new IorException(
              IorDiagnosticCodes.INVALID_OBJECT_URL, "incomplete percent escape");
        }
        output.write((hexValue(value.charAt(index + 1)) << 4) | hexValue(value.charAt(index + 2)));
        index += 2;
      } else if (current <= 0x7F) {
        output.write(current);
      } else {
        throw new IorException(
            IorDiagnosticCodes.INVALID_OBJECT_URL,
            "object URL contains a non-ASCII character: " + current);
      }
    }
    return output.toByteArray();
  }

  private static int hexValue(char character) {
    if (character >= '0' && character <= '9') {
      return character - '0';
    }
    if (character >= 'a' && character <= 'f') {
      return character - 'a' + 10;
    }
    if (character >= 'A' && character <= 'F') {
      return character - 'A' + 10;
    }
    throw new IorException(
        IorDiagnosticCodes.INVALID_OBJECT_URL, "invalid percent escape hex: " + character);
  }

  private static void validateRirExclusivity(List<CorbalocAddress> addresses) {
    long rirCount =
        addresses.stream().filter(address -> address.kind() == CorbalocAddress.Kind.RIR).count();
    if (rirCount > 0 && addresses.size() > 1) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_OBJECT_URL,
          "rir protocol cannot be combined with other addresses");
    }
  }

  private record HostPort(String host, int port) {}
}
