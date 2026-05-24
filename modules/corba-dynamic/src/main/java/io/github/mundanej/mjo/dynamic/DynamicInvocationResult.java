package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Local descriptor-backed dynamic invocation result.
 *
 * @param operation operation descriptor
 * @param value return value, empty for void
 * @param outValues OUT and INOUT values in operation parameter order
 */
public record DynamicInvocationResult(
    IdlOperationDescriptor operation, Optional<AnyValue<?>> value, List<AnyValue<?>> outValues) {

  /** Creates a validated invocation result. */
  public DynamicInvocationResult {
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(value, "value");
    outValues = List.copyOf(Objects.requireNonNull(outValues, "outValues"));
  }

  /** Creates a result with no OUT or INOUT values. */
  public DynamicInvocationResult(IdlOperationDescriptor operation, Optional<AnyValue<?>> value) {
    this(operation, value, List.of());
  }

  /** Creates a void result. */
  public static DynamicInvocationResult voidResult(IdlOperationDescriptor operation) {
    return new DynamicInvocationResult(operation, Optional.empty(), List.of());
  }

  /** Creates a value result. */
  public static DynamicInvocationResult value(IdlOperationDescriptor operation, AnyValue<?> value) {
    return new DynamicInvocationResult(
        operation, Optional.of(Objects.requireNonNull(value, "value")), List.of());
  }

  /** Creates a result with explicit OUT and INOUT values. */
  public static DynamicInvocationResult withOutValues(
      IdlOperationDescriptor operation, Optional<AnyValue<?>> value, List<AnyValue<?>> outValues) {
    return new DynamicInvocationResult(operation, value, outValues);
  }
}
