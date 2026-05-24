package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrException;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopRequest;
import java.util.List;
import java.util.Objects;

/** Encodes and decodes the approved RMI-IIOP GIOP request and reply body slice. */
public final class RmiIiopWireCodec {

  private final RmiCdrOperationCodec operationCodec;

  /** Creates a wire codec backed by explicit repository ID metadata. */
  public RmiIiopWireCodec(RmiRepositoryIdPlan repositoryIdPlan) {
    this(new RmiCdrOperationCodec(repositoryIdPlan));
  }

  /** Creates a wire codec with a caller-supplied operation codec. */
  public RmiIiopWireCodec(RmiCdrOperationCodec operationCodec) {
    this.operationCodec = Objects.requireNonNull(operationCodec, "operationCodec");
  }

  /** Encodes operation arguments as a big-endian request body. */
  public byte[] encodeArguments(RmiIdlOperation operation, List<RmiCdrValue> arguments) {
    try {
      CdrWriter writer = CdrWriter.bigEndian();
      operationCodec.writeArguments(writer, operation, arguments);
      return writer.toByteArray();
    } catch (RuntimeException exception) {
      throw malformed("Could not encode RMI-IIOP request body for " + operation.name(), exception);
    }
  }

  /** Decodes operation arguments from a request body using the request body's byte order. */
  public List<RmiCdrValue> decodeArguments(GiopRequest request, RmiIdlOperation operation) {
    Objects.requireNonNull(request, "request");
    try {
      CdrReader reader = reader(request.header(), request.body());
      List<RmiCdrValue> arguments = operationCodec.readArguments(reader, operation);
      requireFullyConsumed(reader, "request");
      return arguments;
    } catch (RuntimeException exception) {
      throw malformed("Malformed RMI-IIOP request body for " + operation.name(), exception);
    }
  }

  /** Encodes a normal return value as a big-endian reply body. */
  public byte[] encodeReturnValue(RmiIdlOperation operation, RmiCdrValue value) {
    try {
      CdrWriter writer = CdrWriter.bigEndian();
      operationCodec.writeReturnValue(writer, operation, value);
      return writer.toByteArray();
    } catch (RuntimeException exception) {
      throw malformed("Could not encode RMI-IIOP reply body for " + operation.name(), exception);
    }
  }

  /** Decodes a normal return value from a reply body using the reply body's byte order. */
  public RmiCdrValue decodeReturnValue(GiopReply reply, RmiIdlOperation operation) {
    Objects.requireNonNull(reply, "reply");
    try {
      CdrReader reader = reader(reply.header(), reply.body());
      RmiCdrValue value = operationCodec.readReturnValue(reader, operation);
      requireFullyConsumed(reader, "reply");
      return value;
    } catch (RuntimeException exception) {
      throw malformed("Malformed RMI-IIOP reply body for " + operation.name(), exception);
    }
  }

  /** Encodes an approved empty user exception as a repository ID string. */
  public byte[] encodeUserException(RmiIdlOperation operation, RmiIdlExceptionReference exception) {
    return encodeUserException(operation, exception, List.of());
  }

  /** Encodes a declared user exception repository ID and explicit payload fields. */
  public byte[] encodeUserException(
      RmiIdlOperation operation, RmiIdlExceptionReference exception, List<RmiCdrValue> fields) {
    try {
      CdrWriter writer = CdrWriter.bigEndian();
      operationCodec.writeUserException(writer, operation, exception, fields);
      return writer.toByteArray();
    } catch (RuntimeException failure) {
      throw malformed("Could not encode RMI-IIOP user exception for " + operation.name(), failure);
    }
  }

  /** Decodes and validates an approved empty user exception reply body. */
  public RmiCdrUserExceptionPayload decodeUserException(
      GiopReply reply, RmiIdlOperation operation) {
    Objects.requireNonNull(reply, "reply");
    try {
      CdrReader reader = reader(reply.header(), reply.body());
      RmiCdrUserExceptionPayload payload = operationCodec.readUserException(reader, operation);
      requireFullyConsumed(reader, "user exception reply");
      return payload;
    } catch (RmiCdrMarshalingException exception) {
      throw new RmiIiopWireException(
          RmiJavaDiagnosticCodes.UNDECLARED_WIRE_USER_EXCEPTION,
          "Undeclared RMI-IIOP user exception for " + operation.name(),
          exception);
    } catch (RuntimeException exception) {
      throw malformed("Malformed RMI-IIOP user exception reply for " + operation.name(), exception);
    }
  }

  /** Encodes a deterministic local system-failure reply body. */
  public byte[] encodeSystemFailure(RmiIiopWireException exception) {
    Objects.requireNonNull(exception, "exception");
    CdrWriter writer = CdrWriter.bigEndian();
    writer.writeString(exception.code().value());
    writer.writeString(exception.getMessage());
    return writer.toByteArray();
  }

  /** Decodes a deterministic local system-failure reply body. */
  public RmiIiopWireException decodeSystemFailure(GiopReply reply) {
    Objects.requireNonNull(reply, "reply");
    try {
      CdrReader reader = reader(reply.header(), reply.body());
      DiagnosticCode code = codeFor(reader.readString());
      String message = reader.readString();
      requireFullyConsumed(reader, "system exception reply");
      return new RmiIiopWireException(code, message);
    } catch (RuntimeException exception) {
      return new RmiIiopWireException(
          RmiJavaDiagnosticCodes.REMOTE_SYSTEM_EXCEPTION_REPLY,
          "Malformed RMI-IIOP system exception reply",
          exception);
    }
  }

  private static CdrReader reader(GiopHeader header, byte[] body) {
    CdrByteOrder byteOrder =
        header.littleEndian() ? CdrByteOrder.LITTLE_ENDIAN : CdrByteOrder.BIG_ENDIAN;
    return new CdrReader(byteOrder, body);
  }

  private static void requireFullyConsumed(CdrReader reader, String bodyName) {
    if (reader.remaining() != 0) {
      throw new RmiIiopWireException(
          RmiJavaDiagnosticCodes.MALFORMED_WIRE_BODY,
          "RMI-IIOP " + bodyName + " has trailing octets: " + reader.remaining());
    }
  }

  private static RmiIiopWireException malformed(String message, RuntimeException exception) {
    if (exception instanceof RmiIiopWireException wireException) {
      return wireException;
    }
    if (exception instanceof CdrException || exception instanceof RmiCdrMarshalingException) {
      return new RmiIiopWireException(
          RmiJavaDiagnosticCodes.MALFORMED_WIRE_BODY, message, exception);
    }
    return new RmiIiopWireException(RmiJavaDiagnosticCodes.MALFORMED_WIRE_BODY, message, exception);
  }

  private static DiagnosticCode codeFor(String value) {
    if (RmiJavaDiagnosticCodes.INVALID_WIRE_OBJECT_KEY.value().equals(value)) {
      return RmiJavaDiagnosticCodes.INVALID_WIRE_OBJECT_KEY;
    }
    if (RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OBJECT_KEY.value().equals(value)) {
      return RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OBJECT_KEY;
    }
    if (RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OPERATION.value().equals(value)) {
      return RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OPERATION;
    }
    if (RmiJavaDiagnosticCodes.MALFORMED_WIRE_BODY.value().equals(value)) {
      return RmiJavaDiagnosticCodes.MALFORMED_WIRE_BODY;
    }
    if (RmiJavaDiagnosticCodes.UNDECLARED_WIRE_USER_EXCEPTION.value().equals(value)) {
      return RmiJavaDiagnosticCodes.UNDECLARED_WIRE_USER_EXCEPTION;
    }
    if (RmiJavaDiagnosticCodes.UNSUPPORTED_WIRE_REPLY_STATUS.value().equals(value)) {
      return RmiJavaDiagnosticCodes.UNSUPPORTED_WIRE_REPLY_STATUS;
    }
    if (RmiJavaDiagnosticCodes.REMOTE_SYSTEM_EXCEPTION_REPLY.value().equals(value)) {
      return RmiJavaDiagnosticCodes.REMOTE_SYSTEM_EXCEPTION_REPLY;
    }
    return RmiJavaDiagnosticCodes.REMOTE_SYSTEM_EXCEPTION_REPLY;
  }
}
