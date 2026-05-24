package io.github.mundanej.mjo.interceptors;

import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopServiceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Mutable request information exposed to local client-side interceptors. */
public final class ClientRequestContext {

  private static final long MAX_UNSIGNED_LONG = 0xFFFF_FFFFL;

  private final long requestId;
  private final String operation;
  private final List<GiopServiceContext> requestServiceContexts;
  private List<GiopServiceContext> replyServiceContexts = List.of();
  private GiopReplyStatus replyStatus;

  /** Creates client request information for one outgoing request. */
  public ClientRequestContext(
      long requestId, String operation, List<GiopServiceContext> requestServiceContexts) {
    this.requestId = requireUnsignedLong(requestId, "requestId");
    this.operation = requireNonBlank(operation, "operation");
    this.requestServiceContexts =
        new ArrayList<>(Objects.requireNonNull(requestServiceContexts, "requestServiceContexts"));
  }

  /** Returns the unsigned GIOP request ID. */
  public long requestId() {
    return requestId;
  }

  /** Returns the IDL operation name. */
  public String operation() {
    return operation;
  }

  /** Adds or replaces an outgoing request service context. */
  public void addRequestServiceContext(GiopServiceContext context, boolean replace) {
    addServiceContext(requestServiceContexts, context, replace);
  }

  /** Returns outgoing request service contexts in deterministic order. */
  public List<GiopServiceContext> requestServiceContexts() {
    return List.copyOf(requestServiceContexts);
  }

  /** Records reply information before receive-reply callbacks run. */
  public void completeReply(GiopReplyStatus status, List<GiopServiceContext> serviceContexts) {
    this.replyStatus = Objects.requireNonNull(status, "status");
    this.replyServiceContexts =
        List.copyOf(Objects.requireNonNull(serviceContexts, "serviceContexts"));
  }

  /** Returns the reply status when a reply has been received. */
  public Optional<GiopReplyStatus> replyStatus() {
    return Optional.ofNullable(replyStatus);
  }

  /** Returns reply service contexts. */
  public List<GiopServiceContext> replyServiceContexts() {
    return List.copyOf(replyServiceContexts);
  }

  static void addServiceContext(
      List<GiopServiceContext> contexts, GiopServiceContext context, boolean replace) {
    Objects.requireNonNull(context, "context");
    for (int index = 0; index < contexts.size(); index++) {
      if (contexts.get(index).contextId() == context.contextId()) {
        if (!replace) {
          throw new InterceptorException(
              InterceptorDiagnosticCodes.DUPLICATE_SERVICE_CONTEXT,
              "duplicate service context: " + context.contextId());
        }
        contexts.set(index, context);
        return;
      }
    }
    contexts.add(context);
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
