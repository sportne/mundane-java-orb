package io.github.mundanej.mjo.interceptors;

import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopServiceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Mutable request information exposed to local server-side interceptors. */
public final class ServerRequestContext {

  private static final long MAX_UNSIGNED_LONG = 0xFFFF_FFFFL;

  private final long requestId;
  private final String operation;
  private final byte[] objectKey;
  private final List<GiopServiceContext> requestServiceContexts;
  private final List<GiopServiceContext> replyServiceContexts = new ArrayList<>();
  private GiopReplyStatus replyStatus;

  /** Creates server request information for one incoming request. */
  public ServerRequestContext(
      long requestId,
      String operation,
      byte[] objectKey,
      List<GiopServiceContext> requestServiceContexts) {
    this.requestId = requireUnsignedLong(requestId, "requestId");
    this.operation = requireNonBlank(operation, "operation");
    this.objectKey = Objects.requireNonNull(objectKey, "objectKey").clone();
    this.requestServiceContexts =
        List.copyOf(Objects.requireNonNull(requestServiceContexts, "requestServiceContexts"));
  }

  /** Returns the unsigned GIOP request ID. */
  public long requestId() {
    return requestId;
  }

  /** Returns the IDL operation name. */
  public String operation() {
    return operation;
  }

  /** Returns the target object key. */
  public byte[] objectKey() {
    return objectKey.clone();
  }

  /** Returns request service contexts. */
  public List<GiopServiceContext> requestServiceContexts() {
    return requestServiceContexts;
  }

  /** Adds or replaces an outgoing reply service context. */
  public void addReplyServiceContext(GiopServiceContext context, boolean replace) {
    ClientRequestContext.addServiceContext(replyServiceContexts, context, replace);
  }

  /** Records the reply status before send-reply callbacks run. */
  public void replyStatus(GiopReplyStatus status) {
    this.replyStatus = Objects.requireNonNull(status, "status");
  }

  /** Returns the reply status when one has been assigned. */
  public Optional<GiopReplyStatus> replyStatus() {
    return Optional.ofNullable(replyStatus);
  }

  /** Returns outgoing reply service contexts in deterministic order. */
  public List<GiopServiceContext> replyServiceContexts() {
    return List.copyOf(replyServiceContexts);
  }

  private static long requireUnsignedLong(long value, String name) {
    if (value < 0 || value > MAX_UNSIGNED_LONG) {
      throw new IllegalArgumentException(name + " must be unsigned");
    }
    return value;
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
