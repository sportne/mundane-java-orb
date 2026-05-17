package io.github.mundanej.mjo.poa;

/** Supplies retained servants for missing active-object-map entries. */
@FunctionalInterface
public interface PoaServantActivator {

  /**
   * Incarnates a retained servant for one object id.
   *
   * @throws Exception when activation fails
   */
  Object incarnate(Poa poa, String objectId) throws Exception;
}
