package io.github.mundanej.mjo.giop;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** GIOP 1.2 request message with KeyAddr target support. */
public final class GiopRequest implements GiopMessage {

  private final GiopHeader header;
  private final long requestId;
  private final int responseFlags;
  private final byte[] objectKey;
  private final String operation;
  private final List<GiopServiceContext> serviceContexts;
  private final byte[] body;

  /** Creates a request message. */
  public GiopRequest(
      GiopHeader header,
      long requestId,
      int responseFlags,
      byte[] objectKey,
      String operation,
      List<GiopServiceContext> serviceContexts,
      byte[] body) {
    this.header = GiopModel.requireHeader(header, GiopMessageType.REQUEST);
    GiopModel.requireUnsignedLong(requestId, "requestId");
    GiopModel.requireUnsignedOctet(responseFlags, "responseFlags");
    this.requestId = requestId;
    this.responseFlags = responseFlags;
    this.objectKey = GiopModel.copyBytes(objectKey, "objectKey");
    this.operation = GiopModel.requireNonBlank(operation, "operation");
    this.serviceContexts = List.copyOf(Objects.requireNonNull(serviceContexts, "serviceContexts"));
    this.body = GiopModel.copyBytes(body, "body");
  }

  @Override
  public GiopHeader header() {
    return header;
  }

  public long requestId() {
    return requestId;
  }

  public int responseFlags() {
    return responseFlags;
  }

  public byte[] objectKey() {
    return Arrays.copyOf(objectKey, objectKey.length);
  }

  public String operation() {
    return operation;
  }

  public List<GiopServiceContext> serviceContexts() {
    return serviceContexts;
  }

  public byte[] body() {
    return Arrays.copyOf(body, body.length);
  }
}
