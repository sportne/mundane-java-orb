package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.event.EventPullSupplier;
import io.github.mundanej.mjo.event.EventPushConsumer;
import io.github.mundanej.mjo.event.EventServiceDiagnosticCodes;
import io.github.mundanej.mjo.event.EventServiceException;
import io.github.mundanej.mjo.event.EventServiceOptions;
import io.github.mundanej.mjo.event.LocalEventChannel;
import io.github.mundanej.mjo.event.LocalEventService;
import io.github.mundanej.mjo.event.NetworkEventService;
import io.github.mundanej.mjo.event.NetworkEventServiceClient;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopException;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.naming.server.NetworkNamingClient;
import io.github.mundanej.mjo.naming.server.NetworkNamingService;
import io.github.mundanej.mjo.naming.server.RemoteNamingBindingTarget;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import java.util.ArrayDeque;
import java.util.Optional;

/** Native Image smoke coverage for the supported local Event Service slice. */
public final class EventServiceNativeSmoke {

  private EventServiceNativeSmoke() {}

  /** Runs the Event Service Native Image smoke checks. */
  public static void main(String[] args) {
    AnyValue<String> pushed = stringEvent("pushed");
    AnyValue<String> pulled = stringEvent("pulled");

    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      RecordingPushConsumer consumer = new RecordingPushConsumer();
      channel.consumerAdmin().obtainPushSupplierProxy().connectPushConsumer(consumer);
      var pushConsumer = channel.supplierAdmin().obtainPushConsumerProxy();
      pushConsumer.connectPushSupplier(() -> {});
      pushConsumer.push(pushed);
      SmokeAssertions.requireEquals(pushed, consumer.lastEvent, "local push delivery");

      var pullConsumer = channel.supplierAdmin().obtainPullConsumerProxy();
      pullConsumer.connectPullSupplier(new QueuePullSupplier(pulled));
      var pullSupplier = channel.consumerAdmin().obtainPullSupplierProxy();
      pullSupplier.connectPullConsumer(() -> {});
      SmokeAssertions.requireEquals(pulled, pullSupplier.pull(), "local pull delivery");
    }

    try {
      new EventServiceOptions(0, 1, 1, 1);
      throw new AssertionError("invalid Event Service options were accepted");
    } catch (EventServiceException expected) {
      SmokeAssertions.requireEquals(
          EventServiceDiagnosticCodes.INVALID_LIMIT,
          expected.code(),
          "invalid Event Service limit diagnostic");
    }

    NetworkEventServiceClient client = NetworkEventServiceClient.create(IiopOptions.defaults());
    try (NetworkEventService service =
        NetworkEventService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults())) {
      RecordingPushConsumer consumer = new RecordingPushConsumer();
      service.channel().consumerAdmin().obtainPushSupplierProxy().connectPushConsumer(consumer);
      service
          .channel()
          .supplierAdmin()
          .obtainPullConsumerProxy()
          .connectPullSupplier(new QueuePullSupplier(pulled));

      var supplierAdmin = client.forSuppliers(service.ior());
      var consumerAdmin = client.forConsumers(service.ior());
      var pushConsumer = client.obtainPushConsumer(supplierAdmin);
      var pullSupplier = client.obtainPullSupplier(consumerAdmin);

      client.push(pushConsumer, pushed);
      SmokeAssertions.requireEquals(pushed, consumer.lastEvent, "IIOP push delivery");
      SmokeAssertions.requireEquals(pulled, client.pull(pullSupplier), "IIOP pull delivery");
      SmokeAssertions.require(!client.tryPull(pullSupplier).isPresent(), "IIOP empty try_pull");
    }

    try (NetworkNamingService naming =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient namingClient =
            NetworkNamingClient.connect(naming.ior(), IiopOptions.defaults());
        NetworkEventService service =
            NetworkEventService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults())) {
      service.bindInNaming(namingClient, NamingName.parse("EventService"));
      RemoteNamingBindingTarget target = namingClient.resolve(NamingName.parse("EventService"));
      var supplierAdmin = client.forSuppliers(target.ior());
      var pushConsumer = client.obtainPushConsumer(supplierAdmin);
      RecordingPushConsumer consumer = new RecordingPushConsumer();
      service.channel().consumerAdmin().obtainPushSupplierProxy().connectPushConsumer(consumer);

      client.push(pushConsumer, stringEvent("named"));
      SmokeAssertions.requireEquals(
          stringEvent("named"), consumer.lastEvent, "Naming-resolved push");
    }

    NetworkEventService closed =
        NetworkEventService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
    IiopEndpoint endpoint = closed.endpoint();
    closed.close();
    SmokeAssertions.requireThrows(
        IiopException.class,
        () -> io.github.mundanej.mjo.iiop.IiopClient.connect(endpoint, IiopOptions.defaults()),
        "Event Service close shuts down server");
  }

  private static AnyValue<String> stringEvent(String value) {
    return new AnyValue<>(IdlTypeCode.STRING, value);
  }

  private static final class RecordingPushConsumer implements EventPushConsumer {
    private AnyValue<?> lastEvent;

    @Override
    public void push(AnyValue<?> event) {
      lastEvent = event;
    }

    @Override
    public void disconnectPushConsumer() {}
  }

  private static final class QueuePullSupplier implements EventPullSupplier {
    private final ArrayDeque<AnyValue<?>> events = new ArrayDeque<>();

    private QueuePullSupplier(AnyValue<?> event) {
      events.add(event);
    }

    @Override
    public Optional<AnyValue<?>> pull() {
      return Optional.ofNullable(events.poll());
    }

    @Override
    public Optional<AnyValue<?>> tryPull() {
      return Optional.ofNullable(events.poll());
    }

    @Override
    public void disconnectPullSupplier() {}
  }
}
