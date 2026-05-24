package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.giop.GiopCompletionStatus;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.giop.GiopSystemExceptionBody;
import io.github.mundanej.mjo.giop.GiopTargetAddress;
import io.github.mundanej.mjo.giop.GiopUserExceptionBody;
import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.orb.LocalInvocationUserException;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.SystemException;

/** IIOP request handler that dispatches GIOP requests through a local ORB object reference. */
public final class IiopOrbServerHandler implements IiopRequestHandler {

  private final LocalOrb orb;
  private final Map<String, Binding> bindings;

  private IiopOrbServerHandler(LocalOrb orb, Map<String, Binding> bindings) {
    this.orb = Objects.requireNonNull(orb, "orb");
    this.bindings = Map.copyOf(Objects.requireNonNull(bindings, "bindings"));
  }

  /** Creates a builder for an ORB-backed IIOP request handler. */
  public static Builder builder(LocalOrb orb) {
    return new Builder(orb);
  }

  @Override
  public GiopReply handle(GiopRequest request) {
    Objects.requireNonNull(request, "request");
    try {
      Binding binding = bindingFor(objectKey(request.targetAddress()));
      IiopOperationBinding operation = binding.operation(request.operation());
      return invoke(request, binding, operation);
    } catch (SystemException exception) {
      return systemExceptionReply(request, exception);
    } catch (RuntimeException exception) {
      return systemExceptionReply(
          request,
          new org.omg.CORBA.UNKNOWN(
              exception.getMessage(), exception, 0, CompletionStatus.COMPLETED_MAYBE));
    }
  }

  private GiopReply invoke(GiopRequest request, Binding binding, IiopOperationBinding operation) {
    try {
      List<Object> arguments =
          operation.codec().decodeArguments(operation.operation(), request.body());
      Object result = orb.invoke(binding.reference(), operation.operation(), arguments);
      return new GiopReply(
          replyHeader(request),
          request.requestId(),
          GiopReplyStatus.NO_EXCEPTION,
          request.serviceContexts(),
          operation.codec().encodeReturnValue(operation.operation(), result));
    } catch (LocalInvocationUserException exception) {
      GiopUserExceptionBody body =
          new GiopUserExceptionBody(
              exception.raisedType().repositoryId().orElseThrow().value(),
              operation.codec().encodeUserException(exception));
      return new GiopReply(
          replyHeader(request),
          request.requestId(),
          GiopReplyStatus.USER_EXCEPTION,
          request.serviceContexts(),
          body.toBytes(byteOrder(request)));
    }
  }

  private static byte[] objectKey(GiopTargetAddress targetAddress) {
    return switch (targetAddress.discriminator()) {
      case GiopTargetAddress.KEY_ADDR -> targetAddress.objectKey();
      case GiopTargetAddress.PROFILE_ADDR -> objectKey(targetAddress.profile());
      case GiopTargetAddress.REFERENCE_ADDR -> objectKeyFromReferenceAddr(targetAddress);
      default ->
          throw new org.omg.CORBA.BAD_PARAM(
              "Unsupported IIOP target address: " + targetAddress.discriminator(),
              0,
              CompletionStatus.COMPLETED_NO);
    };
  }

  private static byte[] objectKeyFromReferenceAddr(GiopTargetAddress targetAddress) {
    int selectedIndex;
    try {
      selectedIndex = Math.toIntExact(targetAddress.selectedProfileIndex());
    } catch (ArithmeticException exception) {
      throw new org.omg.CORBA.BAD_PARAM(
          "ReferenceAddr selected profile index is too large",
          exception,
          0,
          CompletionStatus.COMPLETED_NO);
    }
    if (selectedIndex < 0 || selectedIndex >= targetAddress.ior().profiles().size()) {
      throw new org.omg.CORBA.BAD_PARAM(
          "ReferenceAddr selected profile index is out of range", 0, CompletionStatus.COMPLETED_NO);
    }
    return objectKey(targetAddress.ior().profiles().get(selectedIndex));
  }

  private static byte[] objectKey(TaggedProfile profile) {
    IiopProfile iiopProfile =
        profile
            .internetIopProfile()
            .orElseThrow(
                () ->
                    new org.omg.CORBA.BAD_PARAM(
                        "IIOP target address profile is not TAG_INTERNET_IOP",
                        0,
                        CompletionStatus.COMPLETED_NO));
    return iiopProfile.objectKey().octets();
  }

  private Binding bindingFor(byte[] objectKey) {
    Binding binding = bindings.get(new String(objectKey, StandardCharsets.US_ASCII));
    if (binding == null) {
      throw new org.omg.CORBA.OBJECT_NOT_EXIST(
          "Unknown IIOP object key", 0, CompletionStatus.COMPLETED_NO);
    }
    return binding;
  }

  private static GiopReply systemExceptionReply(GiopRequest request, SystemException exception) {
    GiopSystemExceptionBody body =
        new GiopSystemExceptionBody(
            "IDL:omg.org/CORBA/" + exception.getClass().getSimpleName() + ":1.0",
            Integer.toUnsignedLong(exception.minor),
            completionStatus(exception.completed));
    return new GiopReply(
        replyHeader(request),
        request.requestId(),
        GiopReplyStatus.SYSTEM_EXCEPTION,
        request.serviceContexts(),
        body.toBytes(byteOrder(request)));
  }

  private static io.github.mundanej.mjo.cdr.CdrByteOrder byteOrder(GiopRequest request) {
    return request.header().littleEndian()
        ? io.github.mundanej.mjo.cdr.CdrByteOrder.LITTLE_ENDIAN
        : io.github.mundanej.mjo.cdr.CdrByteOrder.BIG_ENDIAN;
  }

  private static GiopHeader replyHeader(GiopRequest request) {
    return new GiopHeader(
        request.header().version(),
        request.header().littleEndian(),
        false,
        GiopMessageType.REPLY,
        0);
  }

  private static GiopCompletionStatus completionStatus(CompletionStatus status) {
    return switch (status) {
      case COMPLETED_YES -> GiopCompletionStatus.COMPLETED_YES;
      case COMPLETED_NO -> GiopCompletionStatus.COMPLETED_NO;
      case COMPLETED_MAYBE -> GiopCompletionStatus.COMPLETED_MAYBE;
    };
  }

  /** Builder for immutable network ORB dispatch handlers. */
  public static final class Builder {

    private final LocalOrb orb;
    private final Map<String, Binding> bindings = new LinkedHashMap<>();

    private Builder(LocalOrb orb) {
      this.orb = Objects.requireNonNull(orb, "orb");
    }

    /** Binds one local object reference and its operation codecs. */
    public Builder bind(
        LocalObjectReference<?> reference, List<IiopOperationBinding> operationBindings) {
      Objects.requireNonNull(reference, "reference");
      String objectId = reference.objectId();
      if (bindings.containsKey(objectId)) {
        throw new IiopException(
            IiopDiagnosticCodes.UNSUPPORTED_MESSAGE, "duplicate IIOP object binding: " + objectId);
      }
      bindings.put(objectId, new Binding(reference, operationBindings));
      return this;
    }

    /** Builds the immutable handler. */
    public IiopOrbServerHandler build() {
      return new IiopOrbServerHandler(orb, bindings);
    }
  }

  private record Binding(
      LocalObjectReference<?> reference, Map<String, IiopOperationBinding> operations) {

    private Binding(LocalObjectReference<?> reference, List<IiopOperationBinding> operations) {
      this(reference, operationMap(operations));
    }

    private IiopOperationBinding operation(String name) {
      IiopOperationBinding operation = operations.get(name);
      if (operation == null) {
        throw new org.omg.CORBA.BAD_OPERATION(
            "Unknown IIOP operation: " + name, 0, CompletionStatus.COMPLETED_NO);
      }
      return operation;
    }

    private static Map<String, IiopOperationBinding> operationMap(
        List<IiopOperationBinding> operations) {
      Map<String, IiopOperationBinding> result = new LinkedHashMap<>();
      for (IiopOperationBinding operation : Objects.requireNonNull(operations, "operations")) {
        if (result.put(operation.operation().name(), operation) != null) {
          throw new IiopException(
              IiopDiagnosticCodes.UNSUPPORTED_MESSAGE,
              "duplicate IIOP operation binding: " + operation.operation().name());
        }
      }
      return Map.copyOf(result);
    }
  }
}
