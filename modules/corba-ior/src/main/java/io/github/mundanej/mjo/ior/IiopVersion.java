package io.github.mundanej.mjo.ior;

/** IIOP profile version. */
public record IiopVersion(int major, int minor) {

  /** IIOP 1.0. */
  public static final IiopVersion V1_0 = new IiopVersion(1, 0);

  /** IIOP 1.1. */
  public static final IiopVersion V1_1 = new IiopVersion(1, 1);

  /** IIOP 1.2. */
  public static final IiopVersion V1_2 = new IiopVersion(1, 2);

  /** Creates an IIOP version whose components fit in octets. */
  public IiopVersion {
    if (major < 0 || major > 0xFF || minor < 0 || minor > 0xFF) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_IIOP_PROFILE,
          "IIOP version components must fit in octets: " + major + "." + minor);
    }
  }

  /** Returns whether this profile version carries tagged components. */
  public boolean carriesTaggedComponents() {
    return major == 1 && minor > 0;
  }

  @Override
  public String toString() {
    return major + "." + minor;
  }
}
