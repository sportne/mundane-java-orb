package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.any.AnyValue;
import java.util.Optional;

/** Result for the supported PullSupplier::try_pull return plus out Any payload. */
public record EventTryPullResult(boolean hasEvent, Optional<AnyValue<?>> event) {

  /** Creates a validated try-pull result. */
  public EventTryPullResult {
    event = event == null ? Optional.empty() : event;
    if (hasEvent && event.isEmpty()) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.NO_EVENT_AVAILABLE, "try_pull result is missing an event");
    }
    if (!hasEvent && event.isPresent()) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.INVALID_PAYLOAD,
          "try_pull result must not carry an event when hasEvent is false");
    }
  }

  /** Creates a try-pull result with one event. */
  public static EventTryPullResult present(AnyValue<?> event) {
    return new EventTryPullResult(true, Optional.of(LocalEventChannel.requirePayload(event)));
  }

  /** Creates an empty try-pull result. */
  public static EventTryPullResult empty() {
    return new EventTryPullResult(false, Optional.empty());
  }
}
