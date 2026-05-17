package io.github.mundanej.mjo.poa;

import io.github.mundanej.mjo.modern.LocalInvocationRequest;

/** Locates non-retained servants for each local request. */
public interface PoaServantLocator {

  /**
   * Locates a servant before dispatching one request.
   *
   * @throws Exception when no servant can be located
   */
  PoaServantLocatorResult preinvoke(Poa poa, String objectId, LocalInvocationRequest request)
      throws Exception;

  /**
   * Observes completion after a located servant handled one request.
   *
   * @throws Exception when locator cleanup fails
   */
  void postinvoke(
      Poa poa,
      String objectId,
      LocalInvocationRequest request,
      PoaServantLocatorResult result,
      Object outcome,
      Throwable failure)
      throws Exception;
}
