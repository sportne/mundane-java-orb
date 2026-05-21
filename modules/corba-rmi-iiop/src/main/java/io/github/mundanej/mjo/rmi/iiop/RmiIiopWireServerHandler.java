package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.iiop.IiopRequestHandler;
import io.github.mundanej.mjo.orb.LocalInvocationUserException;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.omg.CORBA.SystemException;

/** Server-side RMI-IIOP GIOP request bridge for local ORB/POA object references. */
public final class RmiIiopWireServerHandler implements IiopRequestHandler {

  private final LocalOrb orb;
  private final RmiIiopWireCodec codec;
  private final Map<RmiIiopObjectKey, Binding> bindings = new LinkedHashMap<>();

  /** Creates a server handler backed by explicit repository ID metadata. */
  public RmiIiopWireServerHandler(LocalOrb orb, RmiRepositoryIdPlan repositoryIdPlan) {
    this(orb, new RmiIiopWireCodec(repositoryIdPlan));
  }

  /** Creates a server handler backed by a caller-supplied wire codec. */
  public RmiIiopWireServerHandler(LocalOrb orb, RmiIiopWireCodec codec) {
    this.orb = Objects.requireNonNull(orb, "orb");
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  /** Registers one local object reference for RMI-IIOP wire dispatch. */
  public synchronized RmiIiopWireServerHandler register(
      RmiIiopObjectKey objectKey, LocalObjectReference<?> reference, RmiIdlInterface idlInterface) {
    Objects.requireNonNull(objectKey, "objectKey");
    Objects.requireNonNull(reference, "reference");
    Objects.requireNonNull(idlInterface, "idlInterface");
    bindings.put(objectKey, Binding.from(reference, idlInterface));
    return this;
  }

  /** Returns the reply for one decoded GIOP request. */
  @Override
  public synchronized GiopReply handle(GiopRequest request) {
    Objects.requireNonNull(request, "request");
    try {
      return handleChecked(request);
    } catch (RmiIiopWireException exception) {
      return systemReply(request, exception);
    } catch (SystemException exception) {
      return systemReply(
          request,
          new RmiIiopWireException(
              RmiJavaDiagnosticCodes.REMOTE_SYSTEM_EXCEPTION_REPLY,
              "Local CORBA system exception during RMI-IIOP dispatch: " + exception.getMessage(),
              exception));
    } catch (RuntimeException exception) {
      return systemReply(
          request,
          new RmiIiopWireException(
              RmiJavaDiagnosticCodes.REMOTE_SYSTEM_EXCEPTION_REPLY,
              "Local runtime failure during RMI-IIOP dispatch",
              exception));
    }
  }

  private GiopReply handleChecked(GiopRequest request) {
    RmiIiopObjectKey objectKey = RmiIiopObjectKey.fromBytes(request.objectKey());
    Binding binding = bindings.get(objectKey);
    if (binding == null) {
      throw new RmiIiopWireException(
          RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OBJECT_KEY,
          "Unknown RMI-IIOP object key: " + objectKey.value());
    }
    RmiIdlOperation operation = binding.operation(request.operation());
    IdlOperationDescriptor descriptor = binding.descriptor(request.operation());
    List<RmiCdrValue> arguments = codec.decodeArguments(request, operation);
    try {
      Object result =
          orb.invoke(
              binding.reference(), descriptor, arguments.stream().map(RmiCdrValue::value).toList());
      return normalReply(request, operation, result);
    } catch (LocalInvocationUserException exception) {
      return userExceptionReply(request, operation, exception);
    }
  }

  private GiopReply normalReply(GiopRequest request, RmiIdlOperation operation, Object result) {
    RmiCdrValue value =
        operation.returnType().kind() == RmiIdlTypeKind.VOID
            ? RmiCdrValue.voidValue()
            : new RmiCdrValue(operation.returnType(), result);
    return reply(request, GiopReplyStatus.NO_EXCEPTION, codec.encodeReturnValue(operation, value));
  }

  private GiopReply userExceptionReply(
      GiopRequest request, RmiIdlOperation operation, LocalInvocationUserException exception) {
    String javaName = exception.userException().getClass().getName();
    RmiIdlExceptionReference declared =
        operation.exceptions().stream()
            .filter(candidate -> candidate.javaBinaryName().equals(javaName))
            .findFirst()
            .orElseThrow(
                () ->
                    new RmiIiopWireException(
                        RmiJavaDiagnosticCodes.UNDECLARED_WIRE_USER_EXCEPTION,
                        "Undeclared RMI-IIOP user exception: " + javaName));
    return reply(
        request, GiopReplyStatus.USER_EXCEPTION, codec.encodeUserException(operation, declared));
  }

  private GiopReply systemReply(GiopRequest request, RmiIiopWireException exception) {
    return reply(request, GiopReplyStatus.SYSTEM_EXCEPTION, codec.encodeSystemFailure(exception));
  }

  private static GiopReply reply(GiopRequest request, GiopReplyStatus status, byte[] body) {
    return new GiopReply(
        GiopHeader.forType(GiopMessageType.REPLY), request.requestId(), status, List.of(), body);
  }

  private record Binding(
      LocalObjectReference<?> reference,
      Map<String, RmiIdlOperation> operations,
      Map<String, IdlOperationDescriptor> descriptors) {

    private static Binding from(LocalObjectReference<?> reference, RmiIdlInterface idlInterface) {
      Map<String, RmiIdlOperation> operations = new LinkedHashMap<>();
      for (RmiIdlOperation operation : idlInterface.operations()) {
        operations.put(operation.name(), operation);
      }
      Map<String, IdlOperationDescriptor> descriptors = new LinkedHashMap<>();
      for (IdlOperationDescriptor descriptor : reference.descriptor().operations()) {
        descriptors.put(descriptor.name(), descriptor);
      }
      return new Binding(reference, Map.copyOf(operations), Map.copyOf(descriptors));
    }

    private RmiIdlOperation operation(String name) {
      RmiIdlOperation operation = operations.get(name);
      if (operation == null) {
        throw new RmiIiopWireException(
            RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OPERATION, "Unknown RMI-IIOP operation: " + name);
      }
      return operation;
    }

    private IdlOperationDescriptor descriptor(String name) {
      IdlOperationDescriptor descriptor = descriptors.get(name);
      if (descriptor == null) {
        throw new RmiIiopWireException(
            RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OPERATION,
            "No local operation descriptor for RMI-IIOP operation: " + name);
      }
      return descriptor;
    }
  }
}
