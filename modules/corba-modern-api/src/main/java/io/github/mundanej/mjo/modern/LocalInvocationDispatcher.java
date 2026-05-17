package io.github.mundanej.mjo.modern;

/** Generated-skeleton-style dispatcher for local in-process invocation. */
@FunctionalInterface
public interface LocalInvocationDispatcher {

  /** Invokes one generated operation request and returns the generated Java result value. */
  Object invoke(LocalInvocationRequest request);
}
