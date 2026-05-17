package io.github.mundanej.mjo.ior;

import java.util.Objects;

/** Parsed {@code corbaname:} object URL. */
public record CorbanameUrl(CorbalocUrl location, String stringName) {

  private static final String PREFIX = "corbaname:";

  /** Creates a parsed corbaname URL value. */
  public CorbanameUrl {
    Objects.requireNonNull(location, "location");
    Objects.requireNonNull(stringName, "stringName");
  }

  /** Parses a corbaname URL with default bounds. */
  public static CorbanameUrl parse(String value) {
    return parse(value, IorLimits.defaults());
  }

  /** Parses a corbaname URL with caller-supplied bounds. */
  public static CorbanameUrl parse(String value, IorLimits limits) {
    Objects.requireNonNull(value, "value");
    limits.requireWithin(limits.objectUrlCharacters(), value.length());
    if (!value.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
      throw new IorException(IorDiagnosticCodes.INVALID_OBJECT_URL, "corbaname scheme is required");
    }
    String body = value.substring(PREFIX.length());
    int hash = body.indexOf('#');
    String corbalocBody = hash < 0 ? body : body.substring(0, hash);
    String name = hash < 0 ? "" : decodeName(body.substring(hash + 1));
    return new CorbanameUrl(CorbalocUrl.parse("corbaloc:" + corbalocBody, limits), name);
  }

  private static String decodeName(String value) {
    byte[] octets = CorbalocUrl.decodeUrlOctets(value);
    char[] characters = new char[octets.length];
    for (int index = 0; index < octets.length; index++) {
      characters[index] = (char) (octets[index] & 0xFF);
    }
    return new String(characters);
  }
}
