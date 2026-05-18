package io.github.mundanej.mjo.dynamic;

/** Handler for one local descriptor-backed dynamic invocation request. */
@FunctionalInterface
public interface DynamicInvocationTarget {

  /** Invokes a dynamic request and returns a dynamic result. */
  DynamicInvocationResult invoke(DynamicInvocationRequest request) throws Exception;
}
