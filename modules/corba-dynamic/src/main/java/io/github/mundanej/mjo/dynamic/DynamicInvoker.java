package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.orb.LocalInvocationUserException;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.Objects;

/** Local DII-style invoker over the in-process LocalOrb surface. */
public final class DynamicInvoker {

  private final LocalOrb orb;

  /** Creates a dynamic invoker for one local ORB. */
  public DynamicInvoker(LocalOrb orb) {
    this.orb = Objects.requireNonNull(orb, "orb");
  }

  /** Invokes a local object reference using descriptor-backed dynamic arguments. */
  public DynamicInvocationResult invoke(
      LocalObjectReference<?> reference, DynamicInvocationRequest request) {
    Objects.requireNonNull(reference, "reference");
    Objects.requireNonNull(request, "request");
    try {
      Object payload =
          orb.invoke(
              reference,
              request.operationCodec().operation(),
              request.operationCodec().toPayloadArguments(request.arguments()));
      return request.operationCodec().resultFromPayload(payload);
    } catch (LocalInvocationUserException exception) {
      throw new DynamicUserException(
          exception.operation(), exception.raisedType(), exception.userException());
    }
  }
}
