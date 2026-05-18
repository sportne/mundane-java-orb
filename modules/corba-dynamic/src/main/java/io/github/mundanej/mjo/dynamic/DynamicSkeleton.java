package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Local DSI-style adapter from generated local dispatch to a dynamic invocation target. */
public final class DynamicSkeleton {

  private DynamicSkeleton() {}

  /** Creates a generated-style local dispatcher backed by a descriptor-keyed dynamic handler. */
  public static LocalInvocationDispatcher dispatcher(
      IdlGeneratedTypeDescriptor descriptor,
      List<DynamicOperationCodec> operationCodecs,
      DynamicInvocationTarget target) {
    Objects.requireNonNull(descriptor, "descriptor");
    Objects.requireNonNull(target, "target");
    Map<IdlOperationDescriptor, DynamicOperationCodec> codecs = codecsByOperation(operationCodecs);
    return request -> dispatch(descriptor, codecs, target, request);
  }

  private static Object dispatch(
      IdlGeneratedTypeDescriptor descriptor,
      Map<IdlOperationDescriptor, DynamicOperationCodec> codecs,
      DynamicInvocationTarget target,
      LocalInvocationRequest request)
      throws Exception {
    if (!descriptor.equals(request.targetDescriptor())) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH, "local request target descriptor does not match");
    }
    DynamicOperationCodec codec = codecs.get(request.operation());
    if (codec == null) {
      throw new DynamicException(
          DynamicDiagnosticCodes.UNKNOWN_OPERATION,
          "unknown dynamic skeleton operation: " + request.operation().name());
    }
    DynamicInvocationRequest dynamicRequest =
        new DynamicInvocationRequest(codec, codec.toAnyArguments(request.arguments()));
    return codec.payloadFromResult(target.invoke(dynamicRequest));
  }

  private static Map<IdlOperationDescriptor, DynamicOperationCodec> codecsByOperation(
      List<DynamicOperationCodec> operationCodecs) {
    Map<IdlOperationDescriptor, DynamicOperationCodec> codecs = new LinkedHashMap<>();
    for (DynamicOperationCodec codec : Objects.requireNonNull(operationCodecs, "operationCodecs")) {
      codecs.put(codec.operation(), codec);
    }
    return Map.copyOf(codecs);
  }
}
