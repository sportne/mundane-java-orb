package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.giop.GiopSystemExceptionBody;
import io.github.mundanej.mjo.giop.GiopUserExceptionBody;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.UNKNOWN;

/** Generated-stub-facing client bridge for invoking local ORB objects over IIOP. */
public final class IiopOrbClient implements AutoCloseable {

  private final IiopObjectReference reference;
  private final IiopClient client;
  private final AtomicLong nextRequestId = new AtomicLong(1);

  private IiopOrbClient(IiopObjectReference reference, IiopClient client) {
    this.reference = Objects.requireNonNull(reference, "reference");
    this.client = Objects.requireNonNull(client, "client");
  }

  /** Connects to the endpoint carried by the supplied IIOP object reference. */
  public static IiopOrbClient connect(IiopObjectReference reference, IiopOptions options) {
    Objects.requireNonNull(reference, "reference");
    return new IiopOrbClient(reference, IiopClient.connect(reference.endpoint(), options));
  }

  /** Invokes one operation through the underlying IIOP client. */
  public Object invoke(
      IdlOperationDescriptor operation, IiopInvocationCodec codec, List<Object> arguments) {
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(codec, "codec");
    Objects.requireNonNull(arguments, "arguments");
    GiopRequest request =
        new GiopRequest(
            GiopHeader.forType(GiopMessageType.REQUEST),
            nextRequestId.getAndIncrement(),
            3,
            reference.objectKey(),
            operation.name(),
            List.of(),
            codec.encodeArguments(operation, arguments));
    GiopReply reply = client.invoke(request);
    return decodeReply(operation, codec, reply);
  }

  @Override
  public void close() {
    client.close();
  }

  private static Object decodeReply(
      IdlOperationDescriptor operation, IiopInvocationCodec codec, GiopReply reply) {
    if (reply.replyStatus() == GiopReplyStatus.NO_EXCEPTION) {
      return codec.decodeReturnValue(operation, reply.body());
    }
    if (reply.replyStatus() == GiopReplyStatus.USER_EXCEPTION) {
      GiopUserExceptionBody body =
          GiopUserExceptionBody.fromBytes(
              reply.header().littleEndian()
                  ? io.github.mundanej.mjo.cdr.CdrByteOrder.LITTLE_ENDIAN
                  : io.github.mundanej.mjo.cdr.CdrByteOrder.BIG_ENDIAN,
              reply.body());
      throw codec.decodeUserException(operation, body.repositoryId(), body.payload());
    }
    if (reply.replyStatus() == GiopReplyStatus.SYSTEM_EXCEPTION) {
      throw systemException(reply);
    }
    throw new IiopException(
        IiopDiagnosticCodes.UNSUPPORTED_MESSAGE,
        "Unsupported IIOP ORB reply status: " + reply.replyStatus());
  }

  private static SystemException systemException(GiopReply reply) {
    GiopSystemExceptionBody body =
        GiopSystemExceptionBody.fromBytes(
            reply.header().littleEndian()
                ? io.github.mundanej.mjo.cdr.CdrByteOrder.LITTLE_ENDIAN
                : io.github.mundanej.mjo.cdr.CdrByteOrder.BIG_ENDIAN,
            reply.body());
    return new UNKNOWN(body.repositoryId(), (int) body.minorCodeValue(), completionStatus(body));
  }

  private static CompletionStatus completionStatus(GiopSystemExceptionBody body) {
    return switch (body.completionStatus()) {
      case COMPLETED_YES -> CompletionStatus.COMPLETED_YES;
      case COMPLETED_NO -> CompletionStatus.COMPLETED_NO;
      case COMPLETED_MAYBE -> CompletionStatus.COMPLETED_MAYBE;
    };
  }
}
