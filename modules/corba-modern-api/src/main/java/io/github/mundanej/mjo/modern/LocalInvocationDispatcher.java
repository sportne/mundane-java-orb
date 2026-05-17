package io.github.mundanej.mjo.modern;

/** Generated-skeleton-style dispatcher for local in-process invocation. */
@FunctionalInterface
public interface LocalInvocationDispatcher {

  /**
   * Invokes one generated operation request and returns the generated Java result value.
   *
   * @throws Exception when the generated servant raises a checked IDL user exception or another
   *     invocation failure
   */
  Object invoke(LocalInvocationRequest request) throws Exception;
}
