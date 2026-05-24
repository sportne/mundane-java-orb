package io.github.mundanej.mjo.giop;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Assembles bounded GIOP fragment sequences into one complete local message. */
public final class GiopFragmentAssembler {

  private final GiopLimits limits;

  /** Creates an assembler with default limits. */
  public GiopFragmentAssembler() {
    this(GiopLimits.defaults());
  }

  /** Creates an assembler with caller-supplied limits. */
  public GiopFragmentAssembler(GiopLimits limits) {
    this.limits = Objects.requireNonNull(limits, "limits");
  }

  /** Assembles an initial message and zero or more following fragment messages. */
  public GiopMessage assemble(List<GiopMessage> messages) {
    Objects.requireNonNull(messages, "messages");
    if (messages.isEmpty()) {
      throw invalid("fragment sequence is empty");
    }
    GiopMessage first = messages.get(0);
    if (first instanceof GiopFragment) {
      throw invalid("fragment sequence is missing an initial message");
    }
    if (!first.header().moreFragments()) {
      if (messages.size() != 1) {
        throw invalid("unfragmented message cannot be followed by fragments");
      }
      return first;
    }
    limits.check(limits.fragmentCount(), messages.size() - 1L);
    List<GiopFragment> fragments = new ArrayList<>(messages.size() - 1);
    for (int index = 1; index < messages.size(); index++) {
      if (!(messages.get(index) instanceof GiopFragment fragment)) {
        throw invalid("fragment sequence contains a non-fragment after the initial message");
      }
      fragments.add(fragment);
    }
    if (fragments.isEmpty() || fragments.get(fragments.size() - 1).header().moreFragments()) {
      throw invalid("fragment sequence is missing a terminating fragment");
    }
    return assembleWithFragments(first, fragments);
  }

  private GiopMessage assembleWithFragments(GiopMessage first, List<GiopFragment> fragments) {
    long requestId = requestId(first);
    int payloadLength = payload(first).length;
    for (GiopFragment fragment : fragments) {
      if (fragment.requestId() != requestId) {
        throw invalid("fragment request id does not match initial message");
      }
      payloadLength = Math.addExact(payloadLength, fragment.fragmentPayload().length);
      limits.check(limits.fragmentBodyOctets(), payloadLength);
    }
    byte[] payload = new byte[payloadLength];
    int position = copy(payload(first), payload, 0);
    for (GiopFragment fragment : fragments) {
      position = copy(fragment.fragmentPayload(), payload, position);
    }
    return withPayload(first, payload);
  }

  private static long requestId(GiopMessage message) {
    if (message instanceof GiopRequest request) {
      return request.requestId();
    }
    if (message instanceof GiopReply reply) {
      return reply.requestId();
    }
    if (message instanceof GiopLocateReply reply) {
      return reply.requestId();
    }
    throw invalid("message type cannot be fragmented by this local assembler");
  }

  private static byte[] payload(GiopMessage message) {
    if (message instanceof GiopRequest request) {
      return request.body();
    }
    if (message instanceof GiopReply reply) {
      return reply.body();
    }
    if (message instanceof GiopLocateReply reply) {
      return reply.body();
    }
    throw invalid("message type has no fragmentable payload");
  }

  private static GiopMessage withPayload(GiopMessage message, byte[] payload) {
    GiopHeader old = message.header();
    GiopHeader header =
        new GiopHeader(old.version(), old.littleEndian(), false, old.messageType(), 0);
    if (message instanceof GiopRequest request) {
      return new GiopRequest(
          header,
          request.requestId(),
          request.responseFlags(),
          request.targetAddress(),
          request.operation(),
          request.serviceContexts(),
          payload);
    }
    if (message instanceof GiopReply reply) {
      return new GiopReply(
          header, reply.requestId(), reply.replyStatus(), reply.serviceContexts(), payload);
    }
    if (message instanceof GiopLocateReply reply) {
      return new GiopLocateReply(header, reply.requestId(), reply.locateStatus(), payload);
    }
    throw invalid("message type has no fragmentable payload");
  }

  private static int copy(byte[] source, byte[] target, int position) {
    System.arraycopy(source, 0, target, position, source.length);
    return position + source.length;
  }

  private static GiopException invalid(String message) {
    return new GiopException(GiopDiagnosticCodes.INVALID_BODY, message);
  }
}
