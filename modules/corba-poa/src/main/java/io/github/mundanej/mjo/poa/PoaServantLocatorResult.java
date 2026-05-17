package io.github.mundanej.mjo.poa;

/** Servant and opaque cookie returned by a non-retained servant locator. */
public record PoaServantLocatorResult(Object servant, Object cookie) {

  /** Creates a validated locator result. */
  public PoaServantLocatorResult {
    servant = PoaExceptions.requireNonNull(servant, "servant");
  }
}
