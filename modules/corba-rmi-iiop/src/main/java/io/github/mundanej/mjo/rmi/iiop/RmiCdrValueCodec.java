package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.Objects;

/** Local CDR codec for one approved RMI-IIOP value/type pair. */
public final class RmiCdrValueCodec {

  /** Writes one value using the supplied expected IDL type. */
  public void writeValue(CdrWriter writer, RmiIdlTypeReference expectedType, RmiCdrValue value) {
    Objects.requireNonNull(writer, "writer");
    Objects.requireNonNull(expectedType, "expectedType");
    Objects.requireNonNull(value, "value");
    requireMatchingType(expectedType, value.type());
    if (expectedType.kind() == RmiIdlTypeKind.VOID) {
      if (value.value() != null) {
        throwTypeMismatch("void values must not carry a Java value");
      }
      return;
    }
    if (value.value() == null) {
      throw new RmiCdrMarshalingException(
          RmiJavaDiagnosticCodes.CDR_NULL_VALUE,
          "Null RMI CDR values are outside the G7-060 marshaling slice");
    }
    requireBuiltin(expectedType);
    writeBuiltin(writer, expectedType.name(), value.value());
  }

  /** Reads one value using the supplied expected IDL type. */
  public RmiCdrValue readValue(CdrReader reader, RmiIdlTypeReference expectedType) {
    Objects.requireNonNull(reader, "reader");
    Objects.requireNonNull(expectedType, "expectedType");
    if (expectedType.kind() == RmiIdlTypeKind.VOID) {
      return RmiCdrValue.voidValue();
    }
    requireBuiltin(expectedType);
    return new RmiCdrValue(expectedType, readBuiltin(reader, expectedType.name()));
  }

  private static void writeBuiltin(CdrWriter writer, String name, Object value) {
    switch (name) {
      case "boolean" -> writer.writeBoolean(requireBoolean(value));
      case "char" -> writer.writeChar(requireCharacter(value));
      case "octet" -> writer.writeOctet(requireByte(value) & 0xFF);
      case "wchar" -> writer.writeUnsignedShort(requireCharacter(value));
      case "short" -> writer.writeShort(requireShort(value));
      case "long" -> writer.writeLong(requireInteger(value));
      case "long long" -> writer.writeLongLong(requireLong(value));
      case "float" -> writer.writeFloat(requireFloat(value));
      case "double" -> writer.writeDouble(requireDouble(value));
      case "string" -> writer.writeString(requireString(value));
      case "wstring" -> writer.writeWString(requireString(value));
      default -> throw unsupported(name);
    }
  }

  private static Object readBuiltin(CdrReader reader, String name) {
    return switch (name) {
      case "boolean" -> reader.readBoolean();
      case "char" -> reader.readChar();
      case "octet" -> (byte) reader.readOctet();
      case "wchar" -> (char) reader.readUnsignedShort();
      case "short" -> reader.readShort();
      case "long" -> reader.readLong();
      case "long long" -> reader.readLongLong();
      case "float" -> reader.readFloat();
      case "double" -> reader.readDouble();
      case "string" -> reader.readString();
      case "wstring" -> reader.readWString();
      default -> throw unsupported(name);
    };
  }

  private static void requireMatchingType(
      RmiIdlTypeReference expectedType, RmiIdlTypeReference actualType) {
    if (!expectedType.equals(actualType)) {
      throwTypeMismatch("RMI CDR value type " + actualType + " does not match " + expectedType);
    }
  }

  private static void requireBuiltin(RmiIdlTypeReference type) {
    if (type.kind() != RmiIdlTypeKind.BUILTIN) {
      throw new RmiCdrMarshalingException(
          RmiJavaDiagnosticCodes.UNSUPPORTED_CDR_MARSHALING_TYPE,
          "Unsupported RMI CDR type kind: " + type.kind());
    }
  }

  private static boolean requireBoolean(Object value) {
    if (value instanceof Boolean typedValue) {
      return typedValue;
    }
    throwTypeMismatch("RMI CDR value must be Boolean");
    return false;
  }

  private static byte requireByte(Object value) {
    if (value instanceof Byte typedValue) {
      return typedValue;
    }
    throwTypeMismatch("RMI CDR value must be Byte");
    return 0;
  }

  private static char requireCharacter(Object value) {
    if (value instanceof Character typedValue) {
      return typedValue;
    }
    throwTypeMismatch("RMI CDR value must be Character");
    return 0;
  }

  private static short requireShort(Object value) {
    if (value instanceof Short typedValue) {
      return typedValue;
    }
    throwTypeMismatch("RMI CDR value must be Short");
    return 0;
  }

  private static int requireInteger(Object value) {
    if (value instanceof Integer typedValue) {
      return typedValue;
    }
    throwTypeMismatch("RMI CDR value must be Integer");
    return 0;
  }

  private static long requireLong(Object value) {
    if (value instanceof Long typedValue) {
      return typedValue;
    }
    throwTypeMismatch("RMI CDR value must be Long");
    return 0L;
  }

  private static float requireFloat(Object value) {
    if (value instanceof Float typedValue) {
      return typedValue;
    }
    throwTypeMismatch("RMI CDR value must be Float");
    return 0.0F;
  }

  private static double requireDouble(Object value) {
    if (value instanceof Double typedValue) {
      return typedValue;
    }
    throwTypeMismatch("RMI CDR value must be Double");
    return 0.0D;
  }

  private static String requireString(Object value) {
    if (value instanceof String typedValue) {
      return typedValue;
    }
    throwTypeMismatch("RMI CDR value must be String");
    return "";
  }

  private static RmiCdrMarshalingException unsupported(String name) {
    return new RmiCdrMarshalingException(
        RmiJavaDiagnosticCodes.UNSUPPORTED_CDR_MARSHALING_TYPE,
        "Unsupported RMI CDR built-in type: " + name);
  }

  private static void throwTypeMismatch(String message) {
    throw new RmiCdrMarshalingException(RmiJavaDiagnosticCodes.CDR_VALUE_TYPE_MISMATCH, message);
  }
}
