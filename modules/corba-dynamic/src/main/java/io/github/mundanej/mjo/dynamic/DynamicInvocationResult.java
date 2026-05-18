package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.Objects;
import java.util.Optional;

/**
 * Local descriptor-backed dynamic invocation result.
 *
 * @param operation operation descriptor
 * @param value return value, empty for void
 */
public record DynamicInvocationResult(
    IdlOperationDescriptor operation, Optional<AnyValue<?>> value) {

  /** Creates a validated invocation result. */
  public DynamicInvocationResult {
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(value, "value");
  }

  /** Creates a void result. */
  public static DynamicInvocationResult voidResult(IdlOperationDescriptor operation) {
    return new DynamicInvocationResult(operation, Optional.empty());
  }

  /** Creates a value result. */
  public static DynamicInvocationResult value(IdlOperationDescriptor operation, AnyValue<?> value) {
    return new DynamicInvocationResult(
        operation, Optional.of(Objects.requireNonNull(value, "value")));
  }
}
