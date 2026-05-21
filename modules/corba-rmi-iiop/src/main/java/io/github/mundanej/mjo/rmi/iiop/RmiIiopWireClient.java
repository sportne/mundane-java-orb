package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.iiop.IiopClient;
import io.github.mundanej.mjo.iiop.IiopException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** RMI-IIOP client bridge over an existing bounded IIOP client. */
public final class RmiIiopWireClient implements AutoCloseable {

  private final IiopClient client;
  private final RmiIiopWireCodec codec;
  private final AtomicLong nextRequestId = new AtomicLong(1L);

  /** Creates an RMI-IIOP wire client backed by explicit repository ID metadata. */
  public RmiIiopWireClient(IiopClient client, RmiRepositoryIdPlan repositoryIdPlan) {
    this(client, new RmiIiopWireCodec(repositoryIdPlan));
  }

  /** Creates an RMI-IIOP wire client backed by a caller-supplied wire codec. */
  public RmiIiopWireClient(IiopClient client, RmiIiopWireCodec codec) {
    this.client = Objects.requireNonNull(client, "client");
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  /** Invokes one approved RMI-IIOP operation over GIOP/IIOP. */
  public RmiCdrValue invoke(
      RmiIiopObjectKey objectKey, RmiIdlOperation operation, List<RmiCdrValue> arguments)
      throws RmiIiopWireUserException {
    Objects.requireNonNull(objectKey, "objectKey");
    Objects.requireNonNull(operation, "operation");
    byte[] body = codec.encodeArguments(operation, arguments);
    GiopRequest request =
        new GiopRequest(
            GiopHeader.forType(GiopMessageType.REQUEST),
            nextRequestId.getAndIncrement(),
            3,
            objectKey.bytes(),
            operation.name(),
            List.of(),
            body);
    GiopReply reply;
    try {
      reply = client.invoke(request);
    } catch (IiopException exception) {
      throw new RmiIiopWireException(
          RmiJavaDiagnosticCodes.MALFORMED_WIRE_BODY,
          "RMI-IIOP transport exchange failed",
          exception);
    }
    return decodeReply(reply, operation);
  }

  /** Closes the underlying IIOP client. */
  @Override
  public void close() {
    client.close();
  }

  private RmiCdrValue decodeReply(GiopReply reply, RmiIdlOperation operation)
      throws RmiIiopWireUserException {
    if (reply.replyStatus() == GiopReplyStatus.NO_EXCEPTION) {
      return codec.decodeReturnValue(reply, operation);
    }
    if (reply.replyStatus() == GiopReplyStatus.USER_EXCEPTION) {
      RmiCdrUserExceptionPayload payload = codec.decodeUserException(reply, operation);
      throw new RmiIiopWireUserException(payload.repositoryId());
    }
    if (reply.replyStatus() == GiopReplyStatus.SYSTEM_EXCEPTION) {
      throw codec.decodeSystemFailure(reply);
    }
    throw new RmiIiopWireException(
        RmiJavaDiagnosticCodes.UNSUPPORTED_WIRE_REPLY_STATUS,
        "Unsupported RMI-IIOP reply status: " + reply.replyStatus());
  }
}
