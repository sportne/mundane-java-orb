package io.github.mundanej.mjo.time;

import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import java.util.Objects;

final class TimeServiceDispatcher implements LocalInvocationDispatcher {

  private final LocalTimeService service;

  TimeServiceDispatcher(LocalTimeService service) {
    this.service = Objects.requireNonNull(service, "service");
  }

  @Override
  public Object invoke(LocalInvocationRequest request) {
    try {
      if (request.operation().equals(TimeServiceDescriptors.UNIVERSAL_TIME)) {
        return service.universalTime();
      }
      if (request.operation().equals(TimeServiceDescriptors.NEW_UNIVERSAL_TIME)) {
        long inaccuracyTicks =
            TimeServiceDescriptors.inaccuracyTicks(
                (Long) request.arguments().get(1), (Integer) request.arguments().get(2));
        return service.newUniversalTime(
            (Long) request.arguments().get(0), inaccuracyTicks, (Short) request.arguments().get(3));
      }
      if (request.operation().equals(TimeServiceDescriptors.NEW_INTERVAL)) {
        return service.newInterval(
            (Long) request.arguments().get(0), (Long) request.arguments().get(1));
      }
      throw TimeServiceCorbaExceptions.badOperation(
          "Unsupported Time Service operation: " + request.operation().name());
    } catch (TimeServiceException exception) {
      throw TimeServiceCorbaExceptions.from(exception);
    }
  }
}
