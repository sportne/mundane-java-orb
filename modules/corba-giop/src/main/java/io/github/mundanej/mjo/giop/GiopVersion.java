package io.github.mundanej.mjo.giop;

/** Supported GIOP protocol version. */
public enum GiopVersion {
  /** GIOP 1.2 message syntax. */
  GIOP_1_2(1, 2);

  private final int major;
  private final int minor;

  GiopVersion(int major, int minor) {
    this.major = major;
    this.minor = minor;
  }

  /** Returns the major version octet. */
  public int major() {
    return major;
  }

  /** Returns the minor version octet. */
  public int minor() {
    return minor;
  }

  static GiopVersion fromOctets(int major, int minor) {
    if (major == GIOP_1_2.major && minor == GIOP_1_2.minor) {
      return GIOP_1_2;
    }
    throw new GiopException(
        GiopDiagnosticCodes.UNSUPPORTED_VERSION,
        "Unsupported GIOP version: " + major + "." + minor);
  }
}
