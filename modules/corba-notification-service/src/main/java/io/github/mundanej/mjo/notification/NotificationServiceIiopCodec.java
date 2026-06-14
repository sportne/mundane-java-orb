package io.github.mundanej.mjo.notification;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.iiop.IiopInvocationCodec;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.orb.LocalInvocationUserException;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CDR codec for the supported CosNotifyChannelAdmin/CosNotifyComm operations. */
public enum NotificationServiceIiopCodec implements IiopInvocationCodec {
  /** Shared stateless codec instance. */
  INSTANCE;

  private static final NotificationStructuredEvent EMPTY_TRY_PULL_RETURN =
      NotificationStructuredEvent.of(
          new NotificationEventIdentity(new NotificationEventType("empty", "empty"), "empty"));

  @Override
  public List<Object> decodeArguments(IdlOperationDescriptor operation, byte[] requestBody) {
    try {
      CdrReader reader = CdrReader.bigEndian(requestBody);
      List<Object> arguments;
      if (operation.equals(NotificationServiceDescriptors.PUSH_STRUCTURED_EVENT)) {
        arguments = List.of(readStructuredEvent(reader));
      } else if (operation.equals(NotificationServiceDescriptors.SET_FILTER)) {
        arguments = List.of(reader.readString());
      } else if (operation.equals(NotificationServiceDescriptors.SET_INTEGER_QOS)) {
        arguments = List.of(reader.readString(), Long.valueOf(reader.readLongLong()));
      } else if (operation.equals(NotificationServiceDescriptors.SET_BOOLEAN_QOS)) {
        arguments = List.of(reader.readString(), Boolean.valueOf(reader.readBoolean()));
      } else {
        requireNoBody(operation, reader);
        arguments = List.of();
      }
      requireFullyRead(reader);
      return arguments;
    } catch (ArithmeticException
        | IllegalArgumentException
        | NotificationServiceException exception) {
      throw NotificationServiceCorbaExceptions.badParam(
          "Invalid Notification Service request body", exception);
    }
  }

  @Override
  public byte[] encodeArguments(IdlOperationDescriptor operation, List<Object> arguments) {
    CdrWriter writer = CdrWriter.bigEndian();
    if (operation.equals(NotificationServiceDescriptors.PUSH_STRUCTURED_EVENT)) {
      requireArgumentCount(operation, arguments, 1);
      writeStructuredEvent(writer, (NotificationStructuredEvent) arguments.get(0));
    } else if (operation.equals(NotificationServiceDescriptors.SET_FILTER)) {
      requireArgumentCount(operation, arguments, 1);
      writer.writeString((String) arguments.get(0));
    } else if (operation.equals(NotificationServiceDescriptors.SET_INTEGER_QOS)) {
      requireArgumentCount(operation, arguments, 2);
      writer
          .writeString((String) arguments.get(0))
          .writeLongLong(((Long) arguments.get(1)).longValue());
    } else if (operation.equals(NotificationServiceDescriptors.SET_BOOLEAN_QOS)) {
      requireArgumentCount(operation, arguments, 2);
      writer
          .writeString((String) arguments.get(0))
          .writeBoolean(((Boolean) arguments.get(1)).booleanValue());
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
    } else if (operation.equals(NotificationServiceDescriptors.PULL_STRUCTURED_EVENT)) {
      writeStructuredEvent(writer, (NotificationStructuredEvent) value);
    } else if (operation.equals(NotificationServiceDescriptors.TRY_PULL_STRUCTURED_EVENT)) {
      NotificationTryPullResult result = (NotificationTryPullResult) value;
      writeStructuredEvent(writer, result.event().orElse(EMPTY_TRY_PULL_RETURN));
      writer.writeBoolean(result.hasEvent());
    } else if (returnsVoid(operation)) {
      if (value != null) {
        throw NotificationServiceCorbaExceptions.badParam(
            "Notification Service void operation returned a value: " + operation.name());
      }
    } else {
      throw NotificationServiceCorbaExceptions.badOperation(
          "Unsupported Notification Service operation: " + operation.name());
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
      } else if (operation.equals(NotificationServiceDescriptors.PULL_STRUCTURED_EVENT)) {
        result = readStructuredEvent(reader);
      } else if (operation.equals(NotificationServiceDescriptors.TRY_PULL_STRUCTURED_EVENT)) {
        NotificationStructuredEvent returnedEvent = readStructuredEvent(reader);
        boolean hasEvent = reader.readBoolean();
        Optional<NotificationStructuredEvent> event =
            hasEvent ? Optional.of(returnedEvent) : Optional.empty();
        result = new NotificationTryPullResult(hasEvent, event);
      } else if (returnsVoid(operation)) {
        result = null;
      } else {
        throw NotificationServiceCorbaExceptions.badOperation(
            "Unsupported Notification Service operation: " + operation.name());
      }
      requireFullyRead(reader);
      return result;
    } catch (ArithmeticException
        | IllegalArgumentException
        | NotificationServiceException exception) {
      throw NotificationServiceCorbaExceptions.badParam(
          "Invalid Notification Service reply body", exception);
    }
  }

  @Override
  public byte[] encodeUserException(LocalInvocationUserException exception) {
    throw NotificationServiceCorbaExceptions.badOperation(
        "Notification Service declares no user exceptions");
  }

  @Override
  public RuntimeException decodeUserException(
      IdlOperationDescriptor operation, String repositoryId, byte[] exceptionBody) {
    return NotificationServiceCorbaExceptions.badOperation(
        "Notification Service declares no user exception: " + repositoryId);
  }

  private static void writeStructuredEvent(CdrWriter writer, NotificationStructuredEvent event) {
    NotificationStructuredEvent checked = LocalNotificationChannel.requireEvent(event);
    writer
        .writeString(checked.identity().eventType().domainName())
        .writeString(checked.identity().eventType().typeName())
        .writeString(checked.identity().eventName());
    writeProperties(writer, checked.filterProperties());
    writeProperties(writer, checked.variableHeaderFields());
    writeProperties(writer, checked.bodyFields());
  }

  private static NotificationStructuredEvent readStructuredEvent(CdrReader reader) {
    NotificationEventIdentity identity =
        new NotificationEventIdentity(
            new NotificationEventType(reader.readString(), reader.readString()),
            reader.readString());
    return new NotificationStructuredEvent(
        identity, readProperties(reader), readProperties(reader), readProperties(reader));
  }

  private static void writeProperties(CdrWriter writer, List<NotificationProperty> properties) {
    writer.writeLong(properties.size());
    for (NotificationProperty property : properties) {
      writer.writeString(property.name()).writeString(property.value().kind().name());
      writePrimitiveValue(writer, property.value());
    }
  }

  private static List<NotificationProperty> readProperties(CdrReader reader) {
    int count = reader.readLong();
    if (count < 0 || count > NotificationStructuredEvent.MAX_FIELDS_PER_SECTION) {
      throw NotificationServiceCorbaExceptions.badParam(
          "Notification Service structured-event property count is out of range: " + count);
    }
    List<NotificationProperty> properties = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      String name = reader.readString();
      NotificationPrimitiveKind kind = NotificationPrimitiveKind.valueOf(reader.readString());
      properties.add(new NotificationProperty(name, readPrimitiveValue(reader, kind)));
    }
    return properties;
  }

  private static void writePrimitiveValue(CdrWriter writer, NotificationPrimitiveValue value) {
    switch (value.kind()) {
      case STRING -> writer.writeString(value.asString());
      case BOOLEAN -> writer.writeBoolean(value.asBoolean());
      case SIGNED_LONG -> writer.writeLongLong(value.asSignedLong());
      case FLOATING_POINT -> writer.writeDouble(value.asFloatingPoint());
    }
  }

  private static NotificationPrimitiveValue readPrimitiveValue(
      CdrReader reader, NotificationPrimitiveKind kind) {
    return switch (kind) {
      case STRING -> NotificationPrimitiveValue.stringValue(reader.readString());
      case BOOLEAN -> NotificationPrimitiveValue.booleanValue(reader.readBoolean());
      case SIGNED_LONG -> NotificationPrimitiveValue.signedLongValue(reader.readLongLong());
      case FLOATING_POINT -> NotificationPrimitiveValue.floatingPointValue(reader.readDouble());
    };
  }

  private static boolean returnsObjectReference(IdlOperationDescriptor operation) {
    return operation.equals(NotificationServiceDescriptors.FOR_SUPPLIERS)
        || operation.equals(NotificationServiceDescriptors.FOR_CONSUMERS)
        || operation.equals(NotificationServiceDescriptors.OBTAIN_STRUCTURED_PUSH_CONSUMER)
        || operation.equals(NotificationServiceDescriptors.OBTAIN_STRUCTURED_PULL_CONSUMER)
        || operation.equals(NotificationServiceDescriptors.OBTAIN_STRUCTURED_PUSH_SUPPLIER)
        || operation.equals(NotificationServiceDescriptors.OBTAIN_STRUCTURED_PULL_SUPPLIER);
  }

  private static boolean returnsVoid(IdlOperationDescriptor operation) {
    return operation.equals(NotificationServiceDescriptors.DESTROY)
        || operation.equals(NotificationServiceDescriptors.PUSH_STRUCTURED_EVENT)
        || operation.equals(NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PUSH_CONSUMER)
        || operation.equals(NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PULL_CONSUMER)
        || operation.equals(NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PUSH_SUPPLIER)
        || operation.equals(NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PULL_SUPPLIER)
        || operation.equals(NotificationServiceDescriptors.SET_FILTER)
        || operation.equals(NotificationServiceDescriptors.SET_INTEGER_QOS)
        || operation.equals(NotificationServiceDescriptors.SET_BOOLEAN_QOS);
  }

  private static void requireArgumentCount(
      IdlOperationDescriptor operation, List<Object> arguments, int expected) {
    if (arguments.size() != expected) {
      throw NotificationServiceCorbaExceptions.badParam(
          "Notification Service operation "
              + operation.name()
              + " expects "
              + expected
              + " argument(s)");
    }
  }

  private static void requireNoBody(IdlOperationDescriptor operation, CdrReader reader) {
    if (reader.remaining() != 0) {
      throw NotificationServiceCorbaExceptions.badParam(
          "Notification Service operation " + operation.name() + " expects an empty request body");
    }
  }

  private static void requireFullyRead(CdrReader reader) {
    if (reader.remaining() != 0) {
      throw NotificationServiceCorbaExceptions.badParam(
          "Notification Service body has trailing octets: " + reader.remaining());
    }
  }
}
