package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.cdr.CdrDiagnosticCodes;
import io.github.mundanej.mjo.cdr.CdrException;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Local CDR codec for one approved RMI-IIOP value/type pair. */
public final class RmiCdrValueCodec {

  private static final int MAX_WSTRING_OCTETS = 65_536;

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
          "Null RMI CDR values are outside the G10-090 marshaling slice");
    }
    switch (expectedType.kind()) {
      case BUILTIN -> writeBuiltin(writer, expectedType.name(), value.value());
      case SEQUENCE -> writeSequence(writer, expectedType, value.value());
      case REMOTE_OBJECT -> writeObjectReference(writer, value.value());
      case DECLARED_VALUE -> writeDeclaredValue(writer, expectedType, value.value());
      case VOID -> throw new IllegalStateException("void handled before value dispatch");
    }
  }

  /** Reads one value using the supplied expected IDL type. */
  public RmiCdrValue readValue(CdrReader reader, RmiIdlTypeReference expectedType) {
    Objects.requireNonNull(reader, "reader");
    Objects.requireNonNull(expectedType, "expectedType");
    if (expectedType.kind() == RmiIdlTypeKind.VOID) {
      return RmiCdrValue.voidValue();
    }
    return switch (expectedType.kind()) {
      case BUILTIN -> new RmiCdrValue(expectedType, readBuiltin(reader, expectedType.name()));
      case SEQUENCE -> readSequence(reader, expectedType);
      case REMOTE_OBJECT ->
          new RmiCdrValue(expectedType, RmiIiopObjectKey.fromString(reader.readString()));
      case DECLARED_VALUE -> readDeclaredValue(reader, expectedType);
      case VOID -> throw new IllegalStateException("void handled before value dispatch");
    };
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
      case "wstring" -> writePeerWString(writer, requireString(value));
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
      case "wstring" -> readPeerWString(reader);
      default -> throw unsupported(name);
    };
  }

  private static void writePeerWString(CdrWriter writer, String value) {
    writer.writeWString(value);
  }

  private static String readPeerWString(CdrReader reader) {
    long octets = reader.readUnsignedLong();
    if (octets > MAX_WSTRING_OCTETS) {
      throw new CdrException(
          CdrDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
          "RMI-IIOP wstring length exceeds " + MAX_WSTRING_OCTETS + " octets: " + octets);
    }
    if (octets == 0) {
      return "";
    }
    if (octets % 2 != 0) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_LENGTH,
          "RMI-IIOP UTF-16 wstring octet length must be even: " + octets);
    }
    int first = reader.readUnsignedShort();
    int codeUnits = Math.toIntExact(octets / 2);
    int offset = 0;
    char[] characters;
    if (first == 0xFFFE) {
      throw new CdrException(
          CdrDiagnosticCodes.MALFORMED_WSTRING, "RMI-IIOP wstring uses byte-swapped UTF-16 marker");
    }
    if (first == 0xFEFF) {
      characters = new char[codeUnits - 1];
    } else {
      characters = new char[codeUnits];
      characters[0] = (char) first;
      offset = 1;
    }
    for (int index = offset; index < characters.length; index++) {
      characters[index] = (char) reader.readUnsignedShort();
    }
    validateUtf16(characters);
    return new String(characters);
  }

  private void writeSequence(CdrWriter writer, RmiIdlTypeReference expectedType, Object value) {
    List<?> values;
    if (value instanceof List<?> typedValues) {
      values = typedValues;
    } else {
      throwTypeMismatch("RMI CDR sequence value must be a List");
      return;
    }
    writer.writeLong(values.size());
    RmiIdlTypeReference elementType = expectedType.elementType().orElseThrow();
    for (Object element : values) {
      RmiCdrValue typedElement;
      if (element instanceof RmiCdrValue rmiCdrValue) {
        typedElement = rmiCdrValue;
      } else {
        throwTypeMismatch("RMI CDR sequence element must be RmiCdrValue");
        return;
      }
      writeValue(writer, elementType, typedElement);
    }
  }

  private RmiCdrValue readSequence(CdrReader reader, RmiIdlTypeReference expectedType) {
    int count = reader.readLong();
    if (count < 0) {
      throw new RmiCdrMarshalingException(
          RmiJavaDiagnosticCodes.MALFORMED_WIRE_BODY,
          "RMI CDR sequence length must not be negative: " + count);
    }
    RmiIdlTypeReference elementType = expectedType.elementType().orElseThrow();
    List<RmiCdrValue> values = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      values.add(readValue(reader, elementType));
    }
    return new RmiCdrValue(expectedType, values);
  }

  private static void writeObjectReference(CdrWriter writer, Object value) {
    if (value instanceof RmiIiopObjectKey objectKey) {
      writer.writeString(objectKey.value());
      return;
    }
    throwTypeMismatch("RMI CDR object-reference value must be RmiIiopObjectKey");
  }

  private void writeDeclaredValue(
      CdrWriter writer, RmiIdlTypeReference expectedType, Object value) {
    RmiCdrDeclaredValue declaredValue;
    if (value instanceof RmiCdrDeclaredValue typedValue) {
      declaredValue = typedValue;
    } else {
      throwTypeMismatch("RMI CDR declared value must be RmiCdrDeclaredValue");
      return;
    }
    writer.writeString(declaredValue.repositoryId());
    writeMembers(writer, expectedType.valueMembers(), declaredValue.members());
  }

  private RmiCdrValue readDeclaredValue(CdrReader reader, RmiIdlTypeReference expectedType) {
    String repositoryId = reader.readString();
    List<RmiCdrValue> members = readMembers(reader, expectedType.valueMembers());
    return new RmiCdrValue(expectedType, new RmiCdrDeclaredValue(repositoryId, members));
  }

  void writeMembers(
      CdrWriter writer, List<RmiIdlValueMember> expectedMembers, List<RmiCdrValue> values) {
    values = List.copyOf(Objects.requireNonNull(values, "values"));
    if (expectedMembers.size() != values.size()) {
      throwTypeMismatch(
          "RMI CDR member count " + values.size() + " does not match " + expectedMembers.size());
    }
    for (int index = 0; index < expectedMembers.size(); index++) {
      writeValue(writer, expectedMembers.get(index).type(), values.get(index));
    }
  }

  List<RmiCdrValue> readMembers(CdrReader reader, List<RmiIdlValueMember> expectedMembers) {
    List<RmiCdrValue> values = new ArrayList<>();
    for (RmiIdlValueMember member : expectedMembers) {
      values.add(readValue(reader, member.type()));
    }
    return List.copyOf(values);
  }

  private static void requireMatchingType(
      RmiIdlTypeReference expectedType, RmiIdlTypeReference actualType) {
    if (!expectedType.equals(actualType)) {
      throwTypeMismatch("RMI CDR value type " + actualType + " does not match " + expectedType);
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

  private static void validateUtf16(char[] characters) {
    for (int index = 0; index < characters.length; index++) {
      char character = characters[index];
      if (Character.isHighSurrogate(character)) {
        if (index + 1 >= characters.length || !Character.isLowSurrogate(characters[index + 1])) {
          throw new CdrException(
              CdrDiagnosticCodes.MALFORMED_WSTRING,
              "RMI-IIOP wstring contains an unmatched high surrogate");
        }
        index++;
      } else if (Character.isLowSurrogate(character)) {
        throw new CdrException(
            CdrDiagnosticCodes.MALFORMED_WSTRING,
            "RMI-IIOP wstring contains an unmatched low surrogate");
      }
    }
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
