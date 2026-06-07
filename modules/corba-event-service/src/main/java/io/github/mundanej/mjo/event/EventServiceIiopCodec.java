package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.any.AnyWireCodec;
import io.github.mundanej.mjo.any.AnyWireValue;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.iiop.IiopInvocationCodec;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.orb.LocalInvocationUserException;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import io.github.mundanej.mjo.typecode.WireTypeCode;
import io.github.mundanej.mjo.typecode.WireTypeCodeKind;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/** CDR codec for the supported CosEventChannelAdmin/CosEventComm IIOP operations. */
public enum EventServiceIiopCodec implements IiopInvocationCodec {
  /** Shared stateless codec instance. */
  INSTANCE;

  private static final AnyWireCodec ANY_CODEC = new AnyWireCodec();
  private static final AnyValue<String> EMPTY_TRY_PULL_RETURN =
      new AnyValue<>(IdlTypeCode.STRING, "");

  @Override
  public List<Object> decodeArguments(IdlOperationDescriptor operation, byte[] requestBody) {
    try {
      CdrReader reader = CdrReader.bigEndian(requestBody);
      List<Object> arguments;
      if (operation.equals(EventServiceDescriptors.PUSH)) {
        arguments = List.of(toLocalAny(ANY_CODEC.read(reader)));
      } else {
        requireNoBody(operation, reader);
        arguments = List.of();
      }
      requireFullyRead(reader);
      return arguments;
    } catch (ArithmeticException | IllegalArgumentException exception) {
      throw EventServiceCorbaExceptions.badParam("Invalid Event Service request body", exception);
    }
  }

  @Override
  public byte[] encodeArguments(IdlOperationDescriptor operation, List<Object> arguments) {
    CdrWriter writer = CdrWriter.bigEndian();
    if (operation.equals(EventServiceDescriptors.PUSH)) {
      requireArgumentCount(operation, arguments, 1);
      ANY_CODEC.write(writer, toWireAny((AnyValue<?>) arguments.get(0)));
    } else {
      requireArgumentCount(operation, arguments, 0);
    }
    return writer.toByteArray();
  }

  @Override
  public byte[] encodeReturnValue(IdlOperationDescriptor operation, Object value) {
    CdrWriter writer = CdrWriter.bigEndian();
    if (returnsObjectReference(operation)) {
      ((Ior) value).writeTo(writer);
    } else if (operation.equals(EventServiceDescriptors.PULL)) {
      ANY_CODEC.write(writer, toWireAny((AnyValue<?>) value));
    } else if (operation.equals(EventServiceDescriptors.TRY_PULL)) {
      EventTryPullResult result = (EventTryPullResult) value;
      ANY_CODEC.write(writer, toWireAny(result.event().orElse(EMPTY_TRY_PULL_RETURN)));
      writer.writeBoolean(result.hasEvent());
    } else if (returnsVoid(operation)) {
      if (value != null) {
        throw EventServiceCorbaExceptions.badParam(
            "Event Service void operation returned a value: " + operation.name());
      }
    } else {
      throw EventServiceCorbaExceptions.badOperation(
          "Unsupported Event Service operation: " + operation.name());
    }
    return writer.toByteArray();
  }

  @Override
  public Object decodeReturnValue(IdlOperationDescriptor operation, byte[] replyBody) {
    try {
      CdrReader reader = CdrReader.bigEndian(replyBody);
      Object result;
      if (returnsObjectReference(operation)) {
        result = Ior.readFrom(reader, io.github.mundanej.mjo.ior.IorLimits.defaults());
      } else if (operation.equals(EventServiceDescriptors.PULL)) {
        result = toLocalAny(ANY_CODEC.read(reader));
      } else if (operation.equals(EventServiceDescriptors.TRY_PULL)) {
        AnyValue<?> returnedEvent = toLocalAny(ANY_CODEC.read(reader));
        boolean hasEvent = reader.readBoolean();
        Optional<AnyValue<?>> event = hasEvent ? Optional.of(returnedEvent) : Optional.empty();
        result = new EventTryPullResult(hasEvent, event);
      } else if (returnsVoid(operation)) {
        result = null;
      } else {
        throw EventServiceCorbaExceptions.badOperation(
            "Unsupported Event Service operation: " + operation.name());
      }
      requireFullyRead(reader);
      return result;
    } catch (ArithmeticException | IllegalArgumentException exception) {
      throw EventServiceCorbaExceptions.badParam("Invalid Event Service reply body", exception);
    }
  }

  @Override
  public byte[] encodeUserException(LocalInvocationUserException exception) {
    throw EventServiceCorbaExceptions.badOperation("Event Service declares no user exceptions");
  }

  @Override
  public RuntimeException decodeUserException(
      IdlOperationDescriptor operation, String repositoryId, byte[] exceptionBody) {
    return EventServiceCorbaExceptions.badOperation(
        "Event Service declares no user exception: " + repositoryId);
  }

  private static AnyWireValue toWireAny(AnyValue<?> value) {
    LocalEventChannel.requirePayload(value);
    IdlTypeCode typeCode = value.typeCode();
    return switch (typeCode.kind()) {
      case BOOLEAN ->
          new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.BOOLEAN), value.value());
      case OCTET -> new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.OCTET), value.value());
      case CHAR -> new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.CHAR), value.value());
      case SHORT -> new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.SHORT), value.value());
      case UNSIGNED_SHORT ->
          new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.UNSIGNED_SHORT), value.value());
      case LONG -> new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.LONG), value.value());
      case UNSIGNED_LONG ->
          new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.UNSIGNED_LONG), value.value());
      case LONG_LONG ->
          new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.LONG_LONG), value.value());
      case UNSIGNED_LONG_LONG ->
          new AnyWireValue(
              WireTypeCode.primitive(WireTypeCodeKind.UNSIGNED_LONG_LONG), value.value());
      case FLOAT -> new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.FLOAT), value.value());
      case DOUBLE ->
          new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.DOUBLE), value.value());
      case LONG_DOUBLE ->
          new AnyWireValue(WireTypeCode.primitive(WireTypeCodeKind.LONG_DOUBLE), value.value());
      case STRING -> new AnyWireValue(WireTypeCode.string(0), value.value());
      default ->
          throw EventServiceCorbaExceptions.badParam(
              "Unsupported Event Service Any TypeCode: " + typeCode.kind());
    };
  }

  private static AnyValue<?> toLocalAny(AnyWireValue value) {
    return switch (value.typeCode().kind()) {
      case BOOLEAN -> new AnyValue<>(IdlTypeCode.BOOLEAN, (Boolean) value.value());
      case OCTET -> new AnyValue<>(IdlTypeCode.OCTET, (Integer) value.value());
      case CHAR -> new AnyValue<>(IdlTypeCode.CHAR, (Character) value.value());
      case SHORT -> new AnyValue<>(IdlTypeCode.SHORT, (Short) value.value());
      case UNSIGNED_SHORT -> new AnyValue<>(IdlTypeCode.UNSIGNED_SHORT, (Integer) value.value());
      case LONG -> new AnyValue<>(IdlTypeCode.LONG, (Integer) value.value());
      case UNSIGNED_LONG -> new AnyValue<>(IdlTypeCode.UNSIGNED_LONG, (Long) value.value());
      case LONG_LONG -> new AnyValue<>(IdlTypeCode.LONG_LONG, (Long) value.value());
      case UNSIGNED_LONG_LONG ->
          new AnyValue<>(IdlTypeCode.UNSIGNED_LONG_LONG, (BigInteger) value.value());
      case FLOAT -> new AnyValue<>(IdlTypeCode.FLOAT, (Float) value.value());
      case DOUBLE -> new AnyValue<>(IdlTypeCode.DOUBLE, (Double) value.value());
      case LONG_DOUBLE -> new AnyValue<>(IdlTypeCode.LONG_DOUBLE, (byte[]) value.value());
      case STRING -> new AnyValue<>(IdlTypeCode.STRING, (String) value.value());
      default ->
          throw EventServiceCorbaExceptions.badParam(
              "Unsupported Event Service wire Any TypeCode: " + value.typeCode().kind());
    };
  }

  private static boolean returnsObjectReference(IdlOperationDescriptor operation) {
    return operation.equals(EventServiceDescriptors.FOR_SUPPLIERS)
        || operation.equals(EventServiceDescriptors.FOR_CONSUMERS)
        || operation.equals(EventServiceDescriptors.OBTAIN_PUSH_CONSUMER)
        || operation.equals(EventServiceDescriptors.OBTAIN_PULL_CONSUMER)
        || operation.equals(EventServiceDescriptors.OBTAIN_PUSH_SUPPLIER)
        || operation.equals(EventServiceDescriptors.OBTAIN_PULL_SUPPLIER);
  }

  private static boolean returnsVoid(IdlOperationDescriptor operation) {
    return operation.equals(EventServiceDescriptors.DESTROY)
        || operation.equals(EventServiceDescriptors.PUSH)
        || operation.equals(EventServiceDescriptors.DISCONNECT_PUSH_CONSUMER)
        || operation.equals(EventServiceDescriptors.DISCONNECT_PULL_CONSUMER)
        || operation.equals(EventServiceDescriptors.DISCONNECT_PUSH_SUPPLIER)
        || operation.equals(EventServiceDescriptors.DISCONNECT_PULL_SUPPLIER);
  }

  private static void requireArgumentCount(
      IdlOperationDescriptor operation, List<Object> arguments, int expected) {
    if (arguments.size() != expected) {
      throw EventServiceCorbaExceptions.badParam(
          "Event Service operation " + operation.name() + " expects " + expected + " argument(s)");
    }
  }

  private static void requireNoBody(IdlOperationDescriptor operation, CdrReader reader) {
    if (reader.remaining() != 0) {
      throw EventServiceCorbaExceptions.badParam(
          "Event Service operation " + operation.name() + " expects an empty request body");
    }
  }

  private static void requireFullyRead(CdrReader reader) {
    if (reader.remaining() != 0) {
      throw EventServiceCorbaExceptions.badParam(
          "Event Service body has trailing octets: " + reader.remaining());
    }
  }
}
