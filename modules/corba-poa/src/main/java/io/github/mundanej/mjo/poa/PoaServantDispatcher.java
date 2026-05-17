package io.github.mundanej.mjo.poa;

import io.github.mundanej.mjo.modern.LocalInvocationRequest;

/** Generated-skeleton-style dispatcher from POA-lite active object map to servant. */
@FunctionalInterface
public interface PoaServantDispatcher<S> {

  /**
   * Invokes one generated operation on a servant.
   *
   * @throws Exception when the generated servant raises a checked IDL user exception or another
   *     invocation failure
   */
  Object invoke(S servant, LocalInvocationRequest request) throws Exception;
}
