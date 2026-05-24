package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Local CDR codec for approved RMI-IIOP operation payloads and empty user exceptions. */
public final class RmiCdrOperationCodec {

  private final RmiCdrValueCodec valueCodec;
  private final Map<String, String> repositoryIdsByJavaName;

  /** Creates an operation codec backed by explicit repository ID metadata. */
  public RmiCdrOperationCodec(RmiRepositoryIdPlan repositoryIdPlan) {
    this(new RmiCdrValueCodec(), repositoryIdPlan);
  }

  /** Creates an operation codec with caller-supplied value codec and repository ID metadata. */
  public RmiCdrOperationCodec(RmiCdrValueCodec valueCodec, RmiRepositoryIdPlan repositoryIdPlan) {
    this.valueCodec = Objects.requireNonNull(valueCodec, "valueCodec");
    Objects.requireNonNull(repositoryIdPlan, "repositoryIdPlan");
    this.repositoryIdsByJavaName = repositoryIdMap(repositoryIdPlan);
  }

  /** Writes operation arguments in declared parameter order. */
  public void writeArguments(
      CdrWriter writer, RmiIdlOperation operation, List<RmiCdrValue> arguments) {
    Objects.requireNonNull(writer, "writer");
    Objects.requireNonNull(operation, "operation");
    arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    if (operation.parameters().size() != arguments.size()) {
      throw new RmiCdrMarshalingException(
          RmiJavaDiagnosticCodes.CDR_OPERATION_ARGUMENT_COUNT_MISMATCH,
          "Operation "
              + operation.name()
              + " expected "
              + operation.parameters().size()
              + " argument(s), got "
              + arguments.size());
    }
    for (int index = 0; index < operation.parameters().size(); index++) {
      valueCodec.writeValue(writer, operation.parameters().get(index).type(), arguments.get(index));
    }
  }

  /** Reads operation arguments in declared parameter order. */
  public List<RmiCdrValue> readArguments(CdrReader reader, RmiIdlOperation operation) {
    Objects.requireNonNull(reader, "reader");
    Objects.requireNonNull(operation, "operation");
    return operation.parameters().stream()
        .map(parameter -> valueCodec.readValue(reader, parameter.type()))
        .toList();
  }

  /** Writes an operation return value, or no payload for {@code void}. */
  public void writeReturnValue(CdrWriter writer, RmiIdlOperation operation, RmiCdrValue value) {
    Objects.requireNonNull(writer, "writer");
    Objects.requireNonNull(operation, "operation");
    valueCodec.writeValue(writer, operation.returnType(), Objects.requireNonNull(value, "value"));
  }

  /** Reads an operation return value, or a void marker for {@code void}. */
  public RmiCdrValue readReturnValue(CdrReader reader, RmiIdlOperation operation) {
    Objects.requireNonNull(reader, "reader");
    Objects.requireNonNull(operation, "operation");
    return valueCodec.readValue(reader, operation.returnType());
  }

  /** Writes the repository ID for an approved empty user exception declared by the operation. */
  public void writeUserException(
      CdrWriter writer, RmiIdlOperation operation, RmiIdlExceptionReference exception) {
    writeUserException(writer, operation, exception, List.of());
  }

  /** Writes a declared user exception repository ID and explicit field payload. */
  public void writeUserException(
      CdrWriter writer,
      RmiIdlOperation operation,
      RmiIdlExceptionReference exception,
      List<RmiCdrValue> fields) {
    Objects.requireNonNull(writer, "writer");
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(exception, "exception");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    if (!operation.exceptions().contains(exception)) {
      throw new RmiCdrMarshalingException(
          RmiJavaDiagnosticCodes.CDR_UNDECLARED_EXCEPTION_REPOSITORY_ID,
          "Exception "
              + exception.javaBinaryName()
              + " is not declared for operation "
              + operation.name());
    }
    writer.writeString(repositoryIdFor(exception));
    valueCodec.writeMembers(writer, exception.fields(), fields);
  }

  /** Reads and validates the repository ID for an approved empty user exception payload. */
  public RmiCdrUserExceptionPayload readUserException(CdrReader reader, RmiIdlOperation operation) {
    Objects.requireNonNull(reader, "reader");
    Objects.requireNonNull(operation, "operation");
    String repositoryId = reader.readString();
    Map<String, RmiIdlExceptionReference> declaredByRepositoryId = new LinkedHashMap<>();
    for (RmiIdlExceptionReference exception : operation.exceptions()) {
      declaredByRepositoryId.put(repositoryIdFor(exception), exception);
    }
    RmiIdlExceptionReference exception = declaredByRepositoryId.get(repositoryId);
    if (exception == null) {
      throw new RmiCdrMarshalingException(
          RmiJavaDiagnosticCodes.CDR_UNDECLARED_EXCEPTION_REPOSITORY_ID,
          "Repository ID is not declared for operation " + operation.name() + ": " + repositoryId);
    }
    return new RmiCdrUserExceptionPayload(
        exception, repositoryId, valueCodec.readMembers(reader, exception.fields()));
  }

  private String repositoryIdFor(RmiIdlExceptionReference exception) {
    return Optional.ofNullable(repositoryIdsByJavaName.get(exception.javaBinaryName()))
        .orElseThrow(
            () ->
                new RmiCdrMarshalingException(
                    RmiJavaDiagnosticCodes.CDR_MISSING_EXCEPTION_REPOSITORY_ID,
                    "Missing repository ID for exception " + exception.javaBinaryName()));
  }

  private static Map<String, String> repositoryIdMap(RmiRepositoryIdPlan repositoryIdPlan) {
    Map<String, String> repositoryIds = new LinkedHashMap<>();
    for (RmiRepositoryIdValue repositoryId : repositoryIdPlan.repositoryIds()) {
      repositoryIds.put(repositoryId.javaBinaryName(), repositoryId.repositoryId());
    }
    return Map.copyOf(repositoryIds);
  }
}
