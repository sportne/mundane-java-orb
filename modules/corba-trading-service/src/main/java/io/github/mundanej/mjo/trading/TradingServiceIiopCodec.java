package io.github.mundanej.mjo.trading;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.iiop.IiopInvocationCodec;
import io.github.mundanej.mjo.orb.LocalInvocationUserException;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CDR codec for the supported CosTrading loopback IIOP operations. */
public enum TradingServiceIiopCodec implements IiopInvocationCodec {
  /** Shared stateless codec instance. */
  INSTANCE;

  @Override
  public List<Object> decodeArguments(IdlOperationDescriptor operation, byte[] requestBody) {
    try {
      CdrReader reader = CdrReader.bigEndian(requestBody);
      List<Object> arguments;
      if (operation.equals(TradingServiceDescriptors.REGISTER_TYPE)
          || operation.equals(TradingServiceDescriptors.UPDATE_TYPE)) {
        arguments = List.of(readServiceType(reader));
      } else if (operation.equals(TradingServiceDescriptors.DELETE_TYPE)
          || operation.equals(TradingServiceDescriptors.LOOKUP_TYPE)
          || operation.equals(TradingServiceDescriptors.WITHDRAW_OFFER)
          || operation.equals(TradingServiceDescriptors.REJECT_REMOTE_IMPORT_QUERY)) {
        arguments = List.of(reader.readString());
      } else if (operation.equals(TradingServiceDescriptors.REGISTER_OFFER)) {
        arguments = List.of(readOffer(reader));
      } else if (operation.equals(TradingServiceDescriptors.QUERY_OFFERS)) {
        arguments = List.of(reader.readString(), reader.readString());
      } else if (operation.equals(TradingServiceDescriptors.REGISTER_IMPORT_LINK)
          || operation.equals(TradingServiceDescriptors.REGISTER_EXPORT_LINK)) {
        arguments = List.of(reader.readString(), reader.readString());
      } else {
        requireNoBody(operation, reader);
        arguments = List.of();
      }
      requireFullyRead(reader);
      return arguments;
    } catch (ArithmeticException | IllegalArgumentException | TradingServiceException exception) {
      throw TradingServiceCorbaExceptions.badParam(
          "Invalid Trading Service request body", exception);
    }
  }

  @Override
  public byte[] encodeArguments(IdlOperationDescriptor operation, List<Object> arguments) {
    CdrWriter writer = CdrWriter.bigEndian();
    if (operation.equals(TradingServiceDescriptors.REGISTER_TYPE)
        || operation.equals(TradingServiceDescriptors.UPDATE_TYPE)) {
      requireArgumentCount(operation, arguments, 1);
      writeServiceType(writer, (TradingServiceType) arguments.get(0));
    } else if (operation.equals(TradingServiceDescriptors.DELETE_TYPE)
        || operation.equals(TradingServiceDescriptors.LOOKUP_TYPE)
        || operation.equals(TradingServiceDescriptors.WITHDRAW_OFFER)
        || operation.equals(TradingServiceDescriptors.REJECT_REMOTE_IMPORT_QUERY)) {
      requireArgumentCount(operation, arguments, 1);
      writer.writeString((String) arguments.get(0));
    } else if (operation.equals(TradingServiceDescriptors.REGISTER_OFFER)) {
      requireArgumentCount(operation, arguments, 1);
      writeOffer(writer, (TradingOffer) arguments.get(0));
    } else if (operation.equals(TradingServiceDescriptors.QUERY_OFFERS)) {
      requireArgumentCount(operation, arguments, 2);
      writer.writeString((String) arguments.get(0)).writeString((String) arguments.get(1));
    } else if (operation.equals(TradingServiceDescriptors.REGISTER_IMPORT_LINK)
        || operation.equals(TradingServiceDescriptors.REGISTER_EXPORT_LINK)) {
      requireArgumentCount(operation, arguments, 2);
      writer.writeString((String) arguments.get(0)).writeString((String) arguments.get(1));
    } else {
      requireArgumentCount(operation, arguments, 0);
    }
    return writer.toByteArray();
  }

  @Override
  public byte[] encodeReturnValue(IdlOperationDescriptor operation, Object value) {
    CdrWriter writer = CdrWriter.bigEndian();
    if (operation.equals(TradingServiceDescriptors.DELETE_TYPE)
        || operation.equals(TradingServiceDescriptors.LOOKUP_TYPE)) {
      writeServiceType(writer, (TradingServiceType) value);
    } else if (operation.equals(TradingServiceDescriptors.LIST_TYPES)) {
      writeServiceTypes(writer, castServiceTypes(value));
    } else if (operation.equals(TradingServiceDescriptors.WITHDRAW_OFFER)) {
      writeOffer(writer, (TradingOffer) value);
    } else if (operation.equals(TradingServiceDescriptors.QUERY_OFFERS)) {
      writeOffers(writer, castOffers(value));
    } else if (operation.equals(TradingServiceDescriptors.LIST_IMPORT_EXPORT_LINKS)) {
      writeLinks(writer, castLinks(value));
    } else if (returnsVoid(operation)) {
      if (value != null) {
        throw TradingServiceCorbaExceptions.badParam(
            "Trading Service void operation returned a value: " + operation.name());
      }
    } else {
      throw unsupported(operation);
    }
    return writer.toByteArray();
  }

  @Override
  public Object decodeReturnValue(IdlOperationDescriptor operation, byte[] replyBody) {
    try {
      CdrReader reader = CdrReader.bigEndian(replyBody);
      Object result;
      if (operation.equals(TradingServiceDescriptors.DELETE_TYPE)
          || operation.equals(TradingServiceDescriptors.LOOKUP_TYPE)) {
        result = readServiceType(reader);
      } else if (operation.equals(TradingServiceDescriptors.LIST_TYPES)) {
        result = readServiceTypes(reader);
      } else if (operation.equals(TradingServiceDescriptors.WITHDRAW_OFFER)) {
        result = readOffer(reader);
      } else if (operation.equals(TradingServiceDescriptors.QUERY_OFFERS)) {
        result = readOffers(reader);
      } else if (operation.equals(TradingServiceDescriptors.LIST_IMPORT_EXPORT_LINKS)) {
        result = readLinks(reader);
      } else if (returnsVoid(operation)) {
        result = null;
      } else {
        throw unsupported(operation);
      }
      requireFullyRead(reader);
      return result;
    } catch (ArithmeticException | IllegalArgumentException | TradingServiceException exception) {
      throw TradingServiceCorbaExceptions.badParam("Invalid Trading Service reply body", exception);
    }
  }

  @Override
  public byte[] encodeUserException(LocalInvocationUserException exception) {
    throw TradingServiceCorbaExceptions.badOperation(
        "Trading Service subset declares no user exceptions");
  }

  @Override
  public RuntimeException decodeUserException(
      IdlOperationDescriptor operation, String repositoryId, byte[] exceptionBody) {
    return TradingServiceCorbaExceptions.badOperation(
        "Trading Service subset declares no user exception: " + repositoryId);
  }

  private static void writeServiceTypes(CdrWriter writer, List<TradingServiceType> types) {
    writer.writeLong(types.size());
    for (TradingServiceType type : types) {
      writeServiceType(writer, type);
    }
  }

  private static List<TradingServiceType> readServiceTypes(CdrReader reader) {
    int count = readCount(reader, TradingServiceOptions.DEFAULT_MAX_TYPES, "service type count");
    List<TradingServiceType> types = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      types.add(readServiceType(reader));
    }
    return types;
  }

  private static void writeServiceType(CdrWriter writer, TradingServiceType type) {
    TradingServiceType checked = new TradingServiceType(type.name(), type.properties());
    writer.writeString(checked.name()).writeLong(checked.properties().size());
    for (TradingPropertyDefinition property : checked.properties()) {
      writer.writeString(property.name()).writeString(property.kind().name());
      writer.writeBoolean(property.required());
    }
  }

  private static TradingServiceType readServiceType(CdrReader reader) {
    String typeName = reader.readString();
    int count =
        readCount(
            reader,
            TradingServiceOptions.DEFAULT_MAX_PROPERTIES_PER_TYPE,
            "property definition count");
    List<TradingPropertyDefinition> properties = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      properties.add(
          new TradingPropertyDefinition(
              reader.readString(),
              TradingPrimitiveKind.valueOf(reader.readString()),
              reader.readBoolean()));
    }
    return new TradingServiceType(typeName, properties);
  }

  private static void writeOffers(CdrWriter writer, List<TradingOffer> offers) {
    writer.writeLong(offers.size());
    for (TradingOffer offer : offers) {
      writeOffer(writer, offer);
    }
  }

  private static List<TradingOffer> readOffers(CdrReader reader) {
    int count = readCount(reader, TradingOfferRepositoryOptions.DEFAULT_MAX_OFFERS, "offer count");
    List<TradingOffer> offers = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      offers.add(readOffer(reader));
    }
    return offers;
  }

  private static void writeOffer(CdrWriter writer, TradingOffer offer) {
    TradingOffer checked = new TradingOffer(offer.id(), offer.typeName(), offer.properties());
    writer.writeString(checked.id()).writeString(checked.typeName());
    writer.writeLong(checked.properties().size());
    for (Map.Entry<String, Object> entry : checked.properties().entrySet()) {
      writer.writeString(entry.getKey());
      writePrimitiveValue(writer, entry.getValue());
    }
  }

  private static TradingOffer readOffer(CdrReader reader) {
    String id = reader.readString();
    String typeName = reader.readString();
    int count =
        readCount(
            reader,
            TradingOfferRepositoryOptions.DEFAULT_MAX_PROPERTIES_PER_OFFER,
            "offer property count");
    Map<String, Object> properties = new LinkedHashMap<>();
    for (int index = 0; index < count; index++) {
      properties.put(reader.readString(), readPrimitiveValue(reader));
    }
    return new TradingOffer(id, typeName, properties);
  }

  private static void writePrimitiveValue(CdrWriter writer, Object value) {
    if (value instanceof String text) {
      writer.writeString(TradingPrimitiveKind.STRING.name()).writeString(text);
    } else if (value instanceof Boolean bool) {
      writer.writeString(TradingPrimitiveKind.BOOLEAN.name()).writeBoolean(bool.booleanValue());
    } else if (value instanceof Long number) {
      writer.writeString(TradingPrimitiveKind.SIGNED_LONG.name()).writeLongLong(number.longValue());
    } else if (value instanceof Double number && Double.isFinite(number.doubleValue())) {
      writer
          .writeString(TradingPrimitiveKind.FLOATING_POINT.name())
          .writeDouble(number.doubleValue());
    } else {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.UNSUPPORTED_VALUE,
          "unsupported Trading Service wire value");
    }
  }

  private static Object readPrimitiveValue(CdrReader reader) {
    TradingPrimitiveKind kind = TradingPrimitiveKind.valueOf(reader.readString());
    return switch (kind) {
      case STRING -> reader.readString();
      case BOOLEAN -> Boolean.valueOf(reader.readBoolean());
      case SIGNED_LONG -> Long.valueOf(reader.readLongLong());
      case FLOATING_POINT -> Double.valueOf(reader.readDouble());
    };
  }

  private static void writeLinks(CdrWriter writer, List<TradingImportExportLink> links) {
    writer.writeLong(links.size());
    for (TradingImportExportLink link : links) {
      writeLink(writer, link);
    }
  }

  private static List<TradingImportExportLink> readLinks(CdrReader reader) {
    int count =
        readCount(reader, TradingImportExportOptions.DEFAULT_MAX_LINKS, "import/export link count");
    List<TradingImportExportLink> links = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      links.add(readLink(reader));
    }
    return links;
  }

  private static void writeLink(CdrWriter writer, TradingImportExportLink link) {
    TradingImportExportLink checked =
        new TradingImportExportLink(link.name(), link.direction(), link.peerTraderName());
    writer.writeString(checked.name()).writeString(checked.direction().name());
    writer.writeString(checked.peerTraderName());
  }

  private static TradingImportExportLink readLink(CdrReader reader) {
    return new TradingImportExportLink(
        reader.readString(),
        TradingImportExportDirection.valueOf(reader.readString()),
        reader.readString());
  }

  @SuppressWarnings("unchecked")
  private static List<TradingServiceType> castServiceTypes(Object value) {
    return (List<TradingServiceType>) value;
  }

  @SuppressWarnings("unchecked")
  private static List<TradingOffer> castOffers(Object value) {
    return (List<TradingOffer>) value;
  }

  @SuppressWarnings("unchecked")
  private static List<TradingImportExportLink> castLinks(Object value) {
    return (List<TradingImportExportLink>) value;
  }

  private static boolean returnsVoid(IdlOperationDescriptor operation) {
    return operation.equals(TradingServiceDescriptors.REGISTER_TYPE)
        || operation.equals(TradingServiceDescriptors.UPDATE_TYPE)
        || operation.equals(TradingServiceDescriptors.REGISTER_OFFER)
        || operation.equals(TradingServiceDescriptors.REGISTER_IMPORT_LINK)
        || operation.equals(TradingServiceDescriptors.REGISTER_EXPORT_LINK)
        || operation.equals(TradingServiceDescriptors.REJECT_REMOTE_IMPORT_QUERY);
  }

  private static int readCount(CdrReader reader, int max, String label) {
    int count = reader.readLong();
    if (count < 0 || count > max) {
      throw TradingServiceCorbaExceptions.badParam(
          "Trading Service " + label + " is out of range: " + count);
    }
    return count;
  }

  private static void requireArgumentCount(
      IdlOperationDescriptor operation, List<Object> arguments, int expected) {
    if (arguments.size() != expected) {
      throw TradingServiceCorbaExceptions.badParam(
          "Trading Service operation "
              + operation.name()
              + " expects "
              + expected
              + " argument(s)");
    }
  }

  private static void requireNoBody(IdlOperationDescriptor operation, CdrReader reader) {
    if (reader.remaining() != 0) {
      throw TradingServiceCorbaExceptions.badParam(
          "Trading Service operation " + operation.name() + " expects an empty request body");
    }
  }

  private static void requireFullyRead(CdrReader reader) {
    if (reader.remaining() != 0) {
      throw TradingServiceCorbaExceptions.badParam(
          "Trading Service body has trailing octets: " + reader.remaining());
    }
  }

  private static org.omg.CORBA.BAD_OPERATION unsupported(IdlOperationDescriptor operation) {
    return TradingServiceCorbaExceptions.badOperation(
        "Unsupported Trading Service operation: " + operation.name());
  }
}
