package io.github.mundanej.mjo.giop;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.common.LimitViolation;
import java.util.Objects;

/**
 * Named bounds for GIOP message parsing and writing.
 *
 * @param messageOctets maximum complete GIOP message octets, including the fixed header
 * @param bodyOctets maximum GIOP body octets declared by the header
 * @param serviceContextCount maximum service contexts in one list
 * @param serviceContextDataOctets maximum opaque context data octets in one context
 */
public record GiopLimits(
    BoundedLimit messageOctets,
    BoundedLimit bodyOctets,
    BoundedLimit serviceContextCount,
    BoundedLimit serviceContextDataOctets,
    BoundedLimit fragmentCount,
    BoundedLimit fragmentBodyOctets) {

  private static final long DEFAULT_MESSAGE_OCTETS = 1_048_576L;
  private static final long DEFAULT_BODY_OCTETS = 1_048_564L;
  private static final long DEFAULT_SERVICE_CONTEXT_COUNT = 64L;
  private static final long DEFAULT_SERVICE_CONTEXT_DATA_OCTETS = 65_536L;
  private static final long DEFAULT_FRAGMENT_COUNT = 256L;
  private static final long DEFAULT_FRAGMENT_BODY_OCTETS = DEFAULT_BODY_OCTETS;

  /** Creates validated GIOP limits. */
  public GiopLimits {
    Objects.requireNonNull(messageOctets, "messageOctets");
    Objects.requireNonNull(bodyOctets, "bodyOctets");
    Objects.requireNonNull(serviceContextCount, "serviceContextCount");
    Objects.requireNonNull(serviceContextDataOctets, "serviceContextDataOctets");
    Objects.requireNonNull(fragmentCount, "fragmentCount");
    Objects.requireNonNull(fragmentBodyOctets, "fragmentBodyOctets");
  }

  /** Creates GIOP limits with default fragment bounds. */
  public GiopLimits(
      BoundedLimit messageOctets,
      BoundedLimit bodyOctets,
      BoundedLimit serviceContextCount,
      BoundedLimit serviceContextDataOctets) {
    this(
        messageOctets,
        bodyOctets,
        serviceContextCount,
        serviceContextDataOctets,
        new BoundedLimit("giop-fragment-count", DEFAULT_FRAGMENT_COUNT),
        new BoundedLimit("giop-fragment-body-octets", DEFAULT_FRAGMENT_BODY_OCTETS));
  }

  /** Returns conservative default GIOP bounds for the first message slice. */
  public static GiopLimits defaults() {
    return new GiopLimits(
        new BoundedLimit("giop-message-octets", DEFAULT_MESSAGE_OCTETS),
        new BoundedLimit("giop-body-octets", DEFAULT_BODY_OCTETS),
        new BoundedLimit("giop-service-context-count", DEFAULT_SERVICE_CONTEXT_COUNT),
        new BoundedLimit("giop-service-context-data-octets", DEFAULT_SERVICE_CONTEXT_DATA_OCTETS),
        new BoundedLimit("giop-fragment-count", DEFAULT_FRAGMENT_COUNT),
        new BoundedLimit("giop-fragment-body-octets", DEFAULT_FRAGMENT_BODY_OCTETS));
  }

  void check(BoundedLimit limit, long observedValue) {
    limit.check(observedValue).ifPresent(GiopLimits::throwLimitExceeded);
  }

  private static void throwLimitExceeded(LimitViolation violation) {
    throw new GiopException(GiopDiagnosticCodes.LIMIT_EXCEEDED, violation.message());
  }
}
