package io.github.mundanej.mjo.notification;

import java.util.Optional;

/** Result for the supported StructuredPullSupplier::try_pull return plus out event. */
public record NotificationTryPullResult(
    boolean hasEvent, Optional<NotificationStructuredEvent> event) {

  /** Creates a validated try-pull result. */
  public NotificationTryPullResult {
    event = event == null ? Optional.empty() : event;
    if (hasEvent && event.isEmpty()) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.NO_EVENT_AVAILABLE,
          "try_pull result is missing a structured event");
    }
    if (!hasEvent && event.isPresent()) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT,
          "try_pull result must not carry a structured event when hasEvent is false");
    }
  }

  /** Creates a try-pull result with one event. */
  public static NotificationTryPullResult present(NotificationStructuredEvent event) {
    return new NotificationTryPullResult(
        true, Optional.of(LocalNotificationChannel.requireEvent(event)));
  }

  /** Creates an empty try-pull result. */
  public static NotificationTryPullResult empty() {
    return new NotificationTryPullResult(false, Optional.empty());
  }
}
