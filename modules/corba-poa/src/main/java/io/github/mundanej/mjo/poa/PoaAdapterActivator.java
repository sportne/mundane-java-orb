package io.github.mundanej.mjo.poa;

/** Creates child POAs during explicit child lookup. */
@FunctionalInterface
public interface PoaAdapterActivator {

  /** Creates or registers a child POA for the requested simple name. */
  Poa createChild(Poa parent, String name);
}
