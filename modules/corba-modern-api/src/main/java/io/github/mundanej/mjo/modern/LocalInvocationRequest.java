package io.github.mundanej.mjo.modern;

import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * In-process generated-code invocation request.
 *
 * @param targetDescriptor static descriptor for the target IDL interface
 * @param operation static descriptor for the invoked IDL operation
 * @param arguments operation arguments in generated Java order
 */
public record LocalInvocationRequest(
    IdlGeneratedTypeDescriptor targetDescriptor,
    IdlOperationDescriptor operation,
    List<Object> arguments) {

  /** Creates a request with immutable argument storage. */
  public LocalInvocationRequest {
    Objects.requireNonNull(targetDescriptor, "targetDescriptor");
    Objects.requireNonNull(operation, "operation");
    arguments =
        Collections.unmodifiableList(
            new ArrayList<>(Objects.requireNonNull(arguments, "arguments")));
  }
}
