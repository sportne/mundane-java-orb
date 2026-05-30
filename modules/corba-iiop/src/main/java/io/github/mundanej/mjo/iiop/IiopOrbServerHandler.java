package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.giop.GiopCompletionStatus;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.giop.GiopServiceContext;
import io.github.mundanej.mjo.giop.GiopSystemExceptionBody;
import io.github.mundanej.mjo.giop.GiopTargetAddress;
import io.github.mundanej.mjo.giop.GiopUserExceptionBody;
import io.github.mundanej.mjo.interceptors.PortableInterceptorRegistry;
import io.github.mundanej.mjo.interceptors.ServerRequestContext;
import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.ObjectKey;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.orb.DurableObjectKey;
import io.github.mundanej.mjo.orb.LocalInvocationUserException;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.SystemException;

/** IIOP request handler that dispatches GIOP requests through a local ORB object reference. */
public final class IiopOrbServerHandler implements IiopRequestHandler {

  private final LocalOrb orb;
  private final Map<ObjectKey, Binding> bindings;
  private final PortableInterceptorRegistry interceptors;

  private IiopOrbServerHandler(
      LocalOrb orb, Map<ObjectKey, Binding> bindings, PortableInterceptorRegistry interceptors) {
    this.orb = Objects.requireNonNull(orb, "orb");
    this.bindings = Map.copyOf(Objects.requireNonNull(bindings, "bindings"));
    this.interceptors = Objects.requireNonNull(interceptors, "interceptors");
  }

  /** Creates a builder for an ORB-backed IIOP request handler. */
  public static Builder builder(LocalOrb orb) {
    return new Builder(orb);
  }

  @Override
  public GiopReply handle(GiopRequest request) {
    Objects.requireNonNull(request, "request");
    ServerRequestContext context = null;
    try {
      byte[] objectKey = objectKey(request.targetAddress());
      context =
          new ServerRequestContext(
              request.requestId(), request.operation(), objectKey, request.serviceContexts());
      interceptors.receiveServerRequestServiceContexts(context);
      Binding binding = bindingFor(objectKey);
      IiopOperationBinding operation = binding.operation(request.operation());
      interceptors.receiveServerRequest(context);
      return invoke(request, context, binding, operation);
    } catch (SystemException exception) {
      ExceptionReply exceptionReply = exceptionReply(context, exception);
      return systemExceptionReply(
          request, exceptionReply.exception(), exceptionReply.serviceContexts());
    } catch (RuntimeException exception) {
      SystemException systemException =
          new org.omg.CORBA.UNKNOWN(
              exception.getMessage(), exception, 0, CompletionStatus.COMPLETED_MAYBE);
      ExceptionReply exceptionReply = exceptionReply(context, systemException);
      return systemExceptionReply(
          request, exceptionReply.exception(), exceptionReply.serviceContexts());
    }
  }

  private ExceptionReply exceptionReply(ServerRequestContext context, SystemException exception) {
    if (context == null) {
      return new ExceptionReply(exception, List.of());
    }
    context.replyStatus(GiopReplyStatus.SYSTEM_EXCEPTION);
    try {
      interceptors.sendServerException(context);
      return new ExceptionReply(exception, context.replyServiceContexts());
    } catch (RuntimeException interceptorFailure) {
      SystemException systemException =
          new org.omg.CORBA.UNKNOWN(
              interceptorFailure.getMessage(),
              interceptorFailure,
              0,
              CompletionStatus.COMPLETED_MAYBE);
      return new ExceptionReply(systemException, List.of());
    }
  }

  private GiopReply invoke(
      GiopRequest request,
      ServerRequestContext context,
      Binding binding,
      IiopOperationBinding operation) {
    try {
      List<Object> arguments =
          operation.codec().decodeArguments(operation.operation(), request.body());
      Object result = orb.invoke(binding.reference(), operation.operation(), arguments);
      byte[] replyBody = operation.codec().encodeReturnValue(operation.operation(), result);
      context.replyStatus(GiopReplyStatus.NO_EXCEPTION);
      try {
        interceptors.sendServerReply(context);
      } catch (RuntimeException interceptorFailure) {
        return interceptorFailureReply(request, interceptorFailure);
      }
      return new GiopReply(
          replyHeader(request),
          request.requestId(),
          GiopReplyStatus.NO_EXCEPTION,
          context.replyServiceContexts(),
          replyBody);
    } catch (LocalInvocationUserException exception) {
      GiopUserExceptionBody body =
          new GiopUserExceptionBody(
              exception.raisedType().repositoryId().orElseThrow().value(),
              operation.codec().encodeUserException(exception));
      byte[] replyBody = body.toBytes(byteOrder(request));
      context.replyStatus(GiopReplyStatus.USER_EXCEPTION);
      try {
        interceptors.sendServerException(context);
      } catch (RuntimeException interceptorFailure) {
        return interceptorFailureReply(request, interceptorFailure);
      }
      return new GiopReply(
          replyHeader(request),
          request.requestId(),
          GiopReplyStatus.USER_EXCEPTION,
          context.replyServiceContexts(),
          replyBody);
    }
  }

  private static GiopReply interceptorFailureReply(GiopRequest request, RuntimeException failure) {
    return systemExceptionReply(
        request,
        new org.omg.CORBA.UNKNOWN(
            failure.getMessage(), failure, 0, CompletionStatus.COMPLETED_MAYBE),
        List.of());
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
    if (DurableObjectKey.hasDurablePrefix(objectKey)) {
      try {
        DurableObjectKey.decode(objectKey);
      } catch (IllegalArgumentException exception) {
        throw new org.omg.CORBA.BAD_PARAM(
            "Malformed durable IIOP object key", exception, 0, CompletionStatus.COMPLETED_NO);
      }
    }
    Binding binding = bindings.get(new ObjectKey(objectKey));
    if (binding == null) {
      throw new org.omg.CORBA.OBJECT_NOT_EXIST(
          "Unknown IIOP object key", 0, CompletionStatus.COMPLETED_NO);
    }
    return binding;
  }

  private static GiopReply systemExceptionReply(
      GiopRequest request, SystemException exception, List<GiopServiceContext> serviceContexts) {
    GiopSystemExceptionBody body =
        new GiopSystemExceptionBody(
            "IDL:omg.org/CORBA/" + exception.getClass().getSimpleName() + ":1.0",
            Integer.toUnsignedLong(exception.minor),
            completionStatus(exception.completed));
    return new GiopReply(
        replyHeader(request),
        request.requestId(),
        GiopReplyStatus.SYSTEM_EXCEPTION,
        serviceContexts,
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
    private final Map<ObjectKey, Binding> bindings = new LinkedHashMap<>();
    private PortableInterceptorRegistry interceptors = PortableInterceptorRegistry.empty();

    private Builder(LocalOrb orb) {
      this.orb = Objects.requireNonNull(orb, "orb");
    }

    /** Binds one local object reference and its operation codecs. */
    public Builder bind(
        LocalObjectReference<?> reference, List<IiopOperationBinding> operationBindings) {
      Objects.requireNonNull(reference, "reference");
      ObjectKey objectKey = new ObjectKey(IiopObjectReference.objectKeyFor(reference));
      if (bindings.containsKey(objectKey)) {
        throw new IiopException(
            IiopDiagnosticCodes.UNSUPPORTED_MESSAGE,
            "duplicate IIOP object binding: " + objectKey.toHex());
      }
      bindings.put(objectKey, new Binding(reference, operationBindings));
      return this;
    }

    /** Configures Portable Interceptors for this server handler. */
    public Builder interceptors(PortableInterceptorRegistry interceptors) {
      this.interceptors = Objects.requireNonNull(interceptors, "interceptors");
      return this;
    }

    /** Builds the immutable handler. */
    public IiopOrbServerHandler build() {
      return new IiopOrbServerHandler(orb, bindings, interceptors);
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

  private record ExceptionReply(
      SystemException exception, List<GiopServiceContext> serviceContexts) {}
}
