package io.github.mundanej.mjo.time;

import io.github.mundanej.mjo.cdr.CdrException;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.iiop.IiopInvocationCodec;
import io.github.mundanej.mjo.orb.LocalInvocationUserException;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.math.BigInteger;
import java.util.List;

/** CDR codec for the supported CosTime TimeService IIOP operations. */
public enum TimeServiceIiopCodec implements IiopInvocationCodec {
  /** Shared stateless codec instance. */
  INSTANCE;

  private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

  @Override
  public List<Object> decodeArguments(IdlOperationDescriptor operation, byte[] requestBody) {
    try {
      CdrReader reader = CdrReader.bigEndian(requestBody);
      List<Object> arguments;
      if (operation.equals(TimeServiceDescriptors.UNIVERSAL_TIME)) {
        arguments = List.of();
      } else if (operation.equals(TimeServiceDescriptors.NEW_UNIVERSAL_TIME)) {
        long time = readSupportedUnsignedLongLong(reader, "time");
        long inacclo = reader.readUnsignedLong();
        int inacchi = reader.readUnsignedShort();
        short tdf = reader.readShort();
        arguments = List.of(time, inacclo, inacchi, tdf);
      } else if (operation.equals(TimeServiceDescriptors.NEW_INTERVAL)) {
        arguments =
            List.of(
                readSupportedUnsignedLongLong(reader, "lower_bound"),
                readSupportedUnsignedLongLong(reader, "upper_bound"));
      } else {
        throw TimeServiceCorbaExceptions.badOperation("Unsupported Time Service operation");
      }
      requireFullyRead(reader);
      return arguments;
    } catch (CdrException | ArithmeticException exception) {
      throw TimeServiceCorbaExceptions.badParam("Invalid Time Service request body", exception);
    }
  }

  @Override
  public byte[] encodeArguments(IdlOperationDescriptor operation, List<Object> arguments) {
    CdrWriter writer = CdrWriter.bigEndian();
    if (operation.equals(TimeServiceDescriptors.UNIVERSAL_TIME)) {
      requireArgumentCount(arguments, 0);
    } else if (operation.equals(TimeServiceDescriptors.NEW_UNIVERSAL_TIME)) {
      requireArgumentCount(arguments, 4);
      long inacclo = ((Long) arguments.get(1)).longValue();
      int inacchi = ((Integer) arguments.get(2)).intValue();
      writeUtcFields(
          writer,
          ((Long) arguments.get(0)).longValue(),
          TimeServiceDescriptors.inaccuracyTicks(inacclo, inacchi),
          ((Short) arguments.get(3)).shortValue());
    } else if (operation.equals(TimeServiceDescriptors.NEW_INTERVAL)) {
      requireArgumentCount(arguments, 2);
      writer.writeUnsignedLongLong(BigInteger.valueOf(((Long) arguments.get(0)).longValue()));
      writer.writeUnsignedLongLong(BigInteger.valueOf(((Long) arguments.get(1)).longValue()));
    } else {
      throw TimeServiceCorbaExceptions.badOperation("Unsupported Time Service operation");
    }
    return writer.toByteArray();
  }

  @Override
  public byte[] encodeReturnValue(IdlOperationDescriptor operation, Object value) {
    CdrWriter writer = CdrWriter.bigEndian();
    if (operation.equals(TimeServiceDescriptors.UNIVERSAL_TIME)
        || operation.equals(TimeServiceDescriptors.NEW_UNIVERSAL_TIME)) {
      UtcTime time = (UtcTime) value;
      writeUtcFields(writer, time.timeTicks(), time.inaccuracyTicks(), time.tdfMinutes());
    } else if (operation.equals(TimeServiceDescriptors.NEW_INTERVAL)) {
      TimeInterval interval = (TimeInterval) value;
      writer.writeUnsignedLongLong(BigInteger.valueOf(interval.lowerBoundTicks()));
      writer.writeUnsignedLongLong(BigInteger.valueOf(interval.upperBoundTicks()));
    } else {
      throw TimeServiceCorbaExceptions.badOperation("Unsupported Time Service operation");
    }
    return writer.toByteArray();
  }

  @Override
  public Object decodeReturnValue(IdlOperationDescriptor operation, byte[] replyBody) {
    try {
      CdrReader reader = CdrReader.bigEndian(replyBody);
      Object result;
      if (operation.equals(TimeServiceDescriptors.UNIVERSAL_TIME)
          || operation.equals(TimeServiceDescriptors.NEW_UNIVERSAL_TIME)) {
        result = readUtcTime(reader);
      } else if (operation.equals(TimeServiceDescriptors.NEW_INTERVAL)) {
        result =
            new TimeInterval(
                readSupportedUnsignedLongLong(reader, "lower_bound"),
                readSupportedUnsignedLongLong(reader, "upper_bound"));
      } else {
        throw TimeServiceCorbaExceptions.badOperation("Unsupported Time Service operation");
      }
      requireFullyRead(reader);
      return result;
    } catch (CdrException | ArithmeticException exception) {
      throw TimeServiceCorbaExceptions.badParam("Invalid Time Service reply body", exception);
    }
  }

  @Override
  public byte[] encodeUserException(LocalInvocationUserException exception) {
    throw TimeServiceCorbaExceptions.badOperation("Time Service declares no user exceptions");
  }

  @Override
  public RuntimeException decodeUserException(
      IdlOperationDescriptor operation, String repositoryId, byte[] exceptionBody) {
    return TimeServiceCorbaExceptions.badOperation(
        "Time Service declares no user exception: " + repositoryId);
  }

  private static UtcTime readUtcTime(CdrReader reader) {
    long time = readSupportedUnsignedLongLong(reader, "time");
    long inacclo = reader.readUnsignedLong();
    int inacchi = reader.readUnsignedShort();
    short tdf = reader.readShort();
    return new UtcTime(time, TimeServiceDescriptors.inaccuracyTicks(inacclo, inacchi), tdf);
  }

  private static void writeUtcFields(
      CdrWriter writer, long timeTicks, long inaccuracyTicks, short tdfMinutes) {
    UtcTime time = new UtcTime(timeTicks, inaccuracyTicks, tdfMinutes);
    writer
        .writeUnsignedLongLong(BigInteger.valueOf(time.timeTicks()))
        .writeUnsignedLong(TimeServiceDescriptors.inacclo(time))
        .writeUnsignedShort(TimeServiceDescriptors.inacchi(time))
        .writeShort(time.tdfMinutes());
  }

  private static long readSupportedUnsignedLongLong(CdrReader reader, String fieldName) {
    BigInteger value = reader.readUnsignedLongLong();
    if (value.compareTo(LONG_MAX) > 0) {
      throw TimeServiceCorbaExceptions.badParam(
          fieldName + " exceeds supported Java long TimeBase range");
    }
    return value.longValueExact();
  }

  private static void requireArgumentCount(List<Object> arguments, int expected) {
    if (arguments.size() != expected) {
      throw TimeServiceCorbaExceptions.badParam(
          "Time Service operation expects " + expected + " argument(s)");
    }
  }

  private static void requireFullyRead(CdrReader reader) {
    if (reader.remaining() != 0) {
      throw TimeServiceCorbaExceptions.badParam(
          "Time Service body has trailing octets: " + reader.remaining());
    }
  }
}
