package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.any.AnyValueCodec;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.iiop.IiopInvocationCodec;
import io.github.mundanej.mjo.orb.LocalInvocationUserException;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Descriptor-backed dynamic invocation codec for the local IIOP dispatch bridge. */
public final class DynamicIiopInvocationCodec implements IiopInvocationCodec {

  private final DynamicOperationCodec operationCodec;

  /** Creates a codec for one dynamic operation. */
  public DynamicIiopInvocationCodec(DynamicOperationCodec operationCodec) {
    this.operationCodec = Objects.requireNonNull(operationCodec, "operationCodec");
  }

  @Override
  public List<Object> decodeArguments(IdlOperationDescriptor operation, byte[] requestBody) {
    requireOperation(operation);
    CdrReader reader = CdrReader.bigEndian(requestBody);
    List<Object> arguments = new ArrayList<>(operationCodec.inputParameterCodecs().size());
    for (AnyValueCodec<?> codec : operationCodec.inputParameterCodecs()) {
      arguments.add(codec.read(reader));
    }
    requireFullyRead(reader);
    return List.copyOf(arguments);
  }

  @Override
  public byte[] encodeArguments(IdlOperationDescriptor operation, List<Object> arguments) {
    requireOperation(operation);
    List<Object> payloads = operationCodec.toPayloadArguments(anyArguments(List.copyOf(arguments)));
    CdrWriter writer = CdrWriter.bigEndian();
    for (int index = 0; index < payloads.size(); index++) {
      writePayload(writer, operationCodec.inputParameterCodecs().get(index), payloads.get(index));
    }
    return writer.toByteArray();
  }

  @Override
  public byte[] encodeReturnValue(IdlOperationDescriptor operation, Object value) {
    requireOperation(operation);
    CdrWriter writer = CdrWriter.bigEndian();
    DynamicInvocationResult result =
        value instanceof DynamicInvocationResult dynamicResult
            ? dynamicResult
            : operationCodec.resultFromPayload(value);
    List<Object> payloads = operationCodec.replyPayloadsFromResult(result);
    int payloadIndex = 0;
    if (operationCodec.returnCodec().isPresent()) {
      writePayload(
          writer, operationCodec.returnCodec().orElseThrow(), payloads.get(payloadIndex++));
    }
    for (AnyValueCodec<?> codec : operationCodec.outParameterCodecs()) {
      writePayload(writer, codec, payloads.get(payloadIndex++));
    }
    return writer.toByteArray();
  }

  @Override
  public Object decodeReturnValue(IdlOperationDescriptor operation, byte[] replyBody) {
    requireOperation(operation);
    if (operationCodec.returnCodec().isEmpty()) {
      if (operationCodec.outParameterCodecs().isEmpty()) {
        if (replyBody.length != 0) {
          throw new DynamicException(
              DynamicDiagnosticCodes.TYPE_MISMATCH, "void dynamic reply has a value body");
        }
        return DynamicInvocationResult.voidResult(operation);
      }
      return decodeReplyWithOutValues(readerForEmpty(replyBody));
    }
    CdrReader reader = CdrReader.bigEndian(replyBody);
    AnyValue<?> value = operationCodec.returnCodec().orElseThrow().readAny(reader);
    List<AnyValue<?>> outValues = readOutValues(reader);
    requireFullyRead(reader);
    return operationCodec.resultFromReplyPayloads(Optional.of(value), outValues);
  }

  @Override
  public byte[] encodeUserException(LocalInvocationUserException exception) {
    Objects.requireNonNull(exception, "exception");
    return new byte[0];
  }

  @Override
  public RuntimeException decodeUserException(
      IdlOperationDescriptor operation, String repositoryId, byte[] exceptionBody) {
    return new DynamicException(
        DynamicDiagnosticCodes.USER_EXCEPTION,
        "dynamic IIOP user exception reply: " + repositoryId);
  }

  private void requireOperation(IdlOperationDescriptor operation) {
    if (!operationCodec.operation().equals(Objects.requireNonNull(operation, "operation"))) {
      throw new DynamicException(
          DynamicDiagnosticCodes.UNKNOWN_OPERATION, "operation does not match dynamic IIOP codec");
    }
  }

  private DynamicInvocationResult decodeReplyWithOutValues(CdrReader reader) {
    List<AnyValue<?>> outValues = readOutValues(reader);
    requireFullyRead(reader);
    return operationCodec.resultFromReplyPayloads(Optional.empty(), outValues);
  }

  private List<AnyValue<?>> readOutValues(CdrReader reader) {
    List<AnyValue<?>> outValues = new ArrayList<>(operationCodec.outParameterCodecs().size());
    for (AnyValueCodec<?> codec : operationCodec.outParameterCodecs()) {
      outValues.add(codec.readAny(reader));
    }
    return List.copyOf(outValues);
  }

  private static CdrReader readerForEmpty(byte[] replyBody) {
    return CdrReader.bigEndian(replyBody);
  }

  private static void requireFullyRead(CdrReader reader) {
    if (reader.remaining() != 0) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS,
          "dynamic IIOP body has trailing octets: " + reader.remaining());
    }
  }

  private static List<AnyValue<?>> anyArguments(List<Object> arguments) {
    List<AnyValue<?>> values = new ArrayList<>(arguments.size());
    for (Object argument : arguments) {
      if (!(argument instanceof AnyValue<?> any)) {
        throw new DynamicException(
            DynamicDiagnosticCodes.TYPE_MISMATCH, "dynamic IIOP arguments must be AnyValue");
      }
      values.add(any);
    }
    return values;
  }

  @SuppressWarnings("unchecked")
  private static <T> void writePayload(CdrWriter writer, AnyValueCodec<T> codec, Object value) {
    try {
      codec.write(writer, (T) value);
    } catch (ClassCastException exception) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH,
          "dynamic IIOP payload does not match codec type",
          exception);
    }
  }
}
