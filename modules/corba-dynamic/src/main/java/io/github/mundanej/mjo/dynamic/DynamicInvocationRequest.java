package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.any.AnyValue;
import java.util.List;
import java.util.Objects;

/**
 * Local descriptor-backed dynamic invocation request.
 *
 * @param operationCodec static operation codec
 * @param arguments dynamic Any arguments in operation order
 */
public record DynamicInvocationRequest(
    DynamicOperationCodec operationCodec, List<AnyValue<?>> arguments) {

  /** Creates an immutable request. */
  public DynamicInvocationRequest {
    Objects.requireNonNull(operationCodec, "operationCodec");
    arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    operationCodec.toPayloadArguments(arguments);
  }
}
