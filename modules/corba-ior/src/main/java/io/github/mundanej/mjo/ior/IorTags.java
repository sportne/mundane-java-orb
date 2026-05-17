package io.github.mundanej.mjo.ior;

/** Standard IOP profile and component tag values used by the first IOR slice. */
public final class IorTags {

  /** Standard IOP tag for IIOP profile data. */
  public static final long TAG_INTERNET_IOP = 0L;

  /** Standard IOP tag for a profile carrying a multiple-component profile. */
  public static final long TAG_MULTIPLE_COMPONENTS = 1L;

  private IorTags() {}
}
