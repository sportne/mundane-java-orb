package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.any.AnyValueCodec;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Static dynamic invocation codec for one descriptor-backed operation.
 *
 * @param operation operation descriptor
 * @param returnCodec return value codec, empty for void operations
 * @param parameterCodecs parameter codecs in operation order
 */
public record DynamicOperationCodec(
    IdlOperationDescriptor operation,
    Optional<AnyValueCodec<?>> returnCodec,
    List<AnyValueCodec<?>> parameterCodecs) {

  /** Creates a validated operation codec. */
  public DynamicOperationCodec {
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(returnCodec, "returnCodec");
    parameterCodecs = List.copyOf(Objects.requireNonNull(parameterCodecs, "parameterCodecs"));
    if (operation.parameters().size() != parameterCodecs.size()) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS,
          "operation "
              + operation.name()
              + " expects "
              + operation.parameters().size()
              + " parameter codec(s), got "
              + parameterCodecs.size());
    }
  }

  /** Creates a codec for a void-return operation. */
  public static DynamicOperationCodec voidReturn(
      IdlOperationDescriptor operation, List<AnyValueCodec<?>> parameterCodecs) {
    return new DynamicOperationCodec(operation, Optional.empty(), parameterCodecs);
  }

  /** Creates a codec for a value-returning operation. */
  public static DynamicOperationCodec valueReturn(
      IdlOperationDescriptor operation,
      AnyValueCodec<?> returnCodec,
      List<AnyValueCodec<?>> parameterCodecs) {
    return new DynamicOperationCodec(
        operation,
        Optional.of(Objects.requireNonNull(returnCodec, "returnCodec")),
        parameterCodecs);
  }

  /** Converts dynamic Any arguments to generated-style Java payload arguments. */
  public List<Object> toPayloadArguments(List<AnyValue<?>> arguments) {
    List<AnyValue<?>> checkedArguments =
        List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    List<AnyValueCodec<?>> inputCodecs = inputParameterCodecs();
    if (checkedArguments.size() != inputCodecs.size()) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS,
          "operation "
              + operation.name()
              + " expects "
              + inputCodecs.size()
              + " argument(s), got "
              + checkedArguments.size());
    }
    List<Object> payloads = new ArrayList<>(checkedArguments.size());
    for (int index = 0; index < checkedArguments.size(); index++) {
      AnyValue<?> argument = checkedArguments.get(index);
      AnyValueCodec<?> codec = inputCodecs.get(index);
      requireType(codec.typeCode(), argument);
      payloads.add(DynamicAnyFactory.checkedPayload(codec.typeCode(), argument.value()));
    }
    return List.copyOf(payloads);
  }

  /** Converts generated-style Java payload arguments to dynamic Any arguments. */
  public List<AnyValue<?>> toAnyArguments(List<Object> payloadArguments) {
    List<Object> payloads =
        List.copyOf(Objects.requireNonNull(payloadArguments, "payloadArguments"));
    List<AnyValueCodec<?>> inputCodecs = inputParameterCodecs();
    if (payloads.size() != inputCodecs.size()) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS,
          "operation "
              + operation.name()
              + " expects "
              + inputCodecs.size()
              + " payload argument(s), got "
              + payloads.size());
    }
    List<AnyValue<?>> result = new ArrayList<>(payloads.size());
    for (int index = 0; index < payloads.size(); index++) {
      result.add(anyFromPayload(inputCodecs.get(index), payloads.get(index)));
    }
    return List.copyOf(result);
  }

  /** Converts a generated-style Java return payload to a dynamic result. */
  public DynamicInvocationResult resultFromPayload(Object payload) {
    if (!outParameters().isEmpty()) {
      if (payload instanceof DynamicInvocationResult result) {
        validateResult(result);
        return result;
      }
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "operation with OUT or INOUT parameters must return a dynamic invocation result");
    }
    if (returnCodec.isEmpty()) {
      if (payload != null) {
        throw new DynamicException(
            DynamicDiagnosticCodes.TYPE_MISMATCH, "void operation returned a value");
      }
      return DynamicInvocationResult.voidResult(operation);
    }
    return DynamicInvocationResult.value(
        operation, anyFromPayload(returnCodec.orElseThrow(), payload));
  }

  /** Converts a dynamic result to a generated-style Java return payload. */
  public Object payloadFromResult(DynamicInvocationResult result) {
    validateResult(result);
    if (!outParameters().isEmpty()) {
      return result;
    }
    return returnPayload(result);
  }

  List<Object> replyPayloadsFromResult(DynamicInvocationResult result) {
    validateResult(result);
    List<AnyValue<?>> outValues = result.outValues();
    List<AnyValueCodec<?>> outCodecs = outParameterCodecs();
    if (outValues.size() != outCodecs.size()) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS,
          "operation "
              + operation.name()
              + " expects "
              + outCodecs.size()
              + " OUT/INOUT value(s), got "
              + outValues.size());
    }
    List<Object> payloads = new ArrayList<>();
    if (returnCodec.isPresent()) {
      payloads.add(returnPayload(result));
    } else if (result.value().isPresent()) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH, "void operation result contains a value");
    }
    for (int index = 0; index < outValues.size(); index++) {
      AnyValue<?> outValue = outValues.get(index);
      AnyValueCodec<?> codec = outCodecs.get(index);
      requireType(codec.typeCode(), outValue);
      payloads.add(DynamicAnyFactory.checkedPayload(codec.typeCode(), outValue.value()));
    }
    return List.copyOf(payloads);
  }

  DynamicInvocationResult resultFromReplyPayloads(
      Optional<AnyValue<?>> returnValue, List<AnyValue<?>> outValues) {
    DynamicInvocationResult result =
        DynamicInvocationResult.withOutValues(operation, returnValue, outValues);
    validateResult(result);
    return result;
  }

  private Object returnPayload(DynamicInvocationResult result) {
    Objects.requireNonNull(result, "result");
    if (!operation.equals(result.operation())) {
      throw new DynamicException(
          DynamicDiagnosticCodes.UNKNOWN_OPERATION, "result operation does not match codec");
    }
    if (returnCodec.isEmpty()) {
      if (result.value().isPresent()) {
        throw new DynamicException(
            DynamicDiagnosticCodes.TYPE_MISMATCH, "void operation result contains a value");
      }
      return null;
    }
    AnyValue<?> value =
        result
            .value()
            .orElseThrow(
                () ->
                    new DynamicException(
                        DynamicDiagnosticCodes.TYPE_MISMATCH,
                        "value-returning operation result is empty"));
    IdlTypeCode typeCode = returnCodec.orElseThrow().typeCode();
    requireType(typeCode, value);
    return DynamicAnyFactory.checkedPayload(typeCode, value.value());
  }

  private void validateResult(DynamicInvocationResult result) {
    Objects.requireNonNull(result, "result");
    if (!operation.equals(result.operation())) {
      throw new DynamicException(
          DynamicDiagnosticCodes.UNKNOWN_OPERATION, "result operation does not match codec");
    }
    if (returnCodec.isEmpty() && result.value().isPresent()) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH, "void operation result contains a value");
    }
    if (returnCodec.isPresent()) {
      AnyValue<?> value =
          result
              .value()
              .orElseThrow(
                  () ->
                      new DynamicException(
                          DynamicDiagnosticCodes.TYPE_MISMATCH,
                          "value-returning operation result is empty"));
      requireType(returnCodec.orElseThrow().typeCode(), value);
      DynamicAnyFactory.checkedPayload(returnCodec.orElseThrow().typeCode(), value.value());
    }
    List<AnyValue<?>> outValues = result.outValues();
    List<AnyValueCodec<?>> outCodecs = outParameterCodecs();
    if (outValues.size() != outCodecs.size()) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS,
          "operation "
              + operation.name()
              + " expects "
              + outCodecs.size()
              + " OUT/INOUT value(s), got "
              + outValues.size());
    }
    for (int index = 0; index < outValues.size(); index++) {
      AnyValue<?> outValue = outValues.get(index);
      AnyValueCodec<?> codec = outCodecs.get(index);
      requireType(codec.typeCode(), outValue);
      DynamicAnyFactory.checkedPayload(codec.typeCode(), outValue.value());
    }
  }

  private static AnyValue<?> anyFromPayload(AnyValueCodec<?> codec, Object payload) {
    Object checkedPayload = DynamicAnyFactory.checkedPayload(codec.typeCode(), payload);
    return new AnyValue<>(codec.typeCode(), checkedPayload);
  }

  private static void requireType(IdlTypeCode expected, AnyValue<?> actual) {
    DynamicAnyFactory.requireType(expected, actual);
  }

  /** Returns OUT and INOUT parameter descriptors in operation order. */
  public List<IdlParameterDescriptor> outParameters() {
    return operation.parameters().stream()
        .filter(
            parameter ->
                parameter.mode() == IdlParameterMode.OUT
                    || parameter.mode() == IdlParameterMode.INOUT)
        .toList();
  }

  List<AnyValueCodec<?>> inputParameterCodecs() {
    return parameterCodecs(IdlParameterMode.IN, IdlParameterMode.INOUT);
  }

  List<AnyValueCodec<?>> outParameterCodecs() {
    return parameterCodecs(IdlParameterMode.OUT, IdlParameterMode.INOUT);
  }

  private List<AnyValueCodec<?>> parameterCodecs(IdlParameterMode first, IdlParameterMode second) {
    List<AnyValueCodec<?>> result = new ArrayList<>();
    for (int index = 0; index < operation.parameters().size(); index++) {
      IdlParameterMode mode = operation.parameters().get(index).mode();
      if (mode == first || mode == second) {
        result.add(parameterCodecs.get(index));
      }
    }
    return List.copyOf(result);
  }
}
