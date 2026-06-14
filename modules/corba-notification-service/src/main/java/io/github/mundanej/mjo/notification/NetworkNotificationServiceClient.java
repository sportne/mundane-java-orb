package io.github.mundanej.mjo.notification;

import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopOrbClient;
import io.github.mundanej.mjo.ior.Ior;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Client helper for invoking the supported Notification Service IIOP operations. */
public final class NetworkNotificationServiceClient {

  private final IiopOptions options;

  private NetworkNotificationServiceClient(IiopOptions options) {
    this.options = Objects.requireNonNull(options, "options");
  }

  /** Creates a client helper with explicit IIOP options. */
  public static NetworkNotificationServiceClient create(IiopOptions options) {
    return new NetworkNotificationServiceClient(options);
  }

  /** Invokes EventChannel::for_suppliers. */
  public Ior forSuppliers(Ior eventChannel) {
    return (Ior) invoke(eventChannel, NotificationServiceDescriptors.FOR_SUPPLIERS, List.of());
  }

  /** Invokes EventChannel::for_consumers. */
  public Ior forConsumers(Ior eventChannel) {
    return (Ior) invoke(eventChannel, NotificationServiceDescriptors.FOR_CONSUMERS, List.of());
  }

  /** Invokes SupplierAdmin::obtain_structured_push_consumer. */
  public Ior obtainStructuredPushConsumer(Ior supplierAdmin) {
    return (Ior)
        invoke(
            supplierAdmin,
            NotificationServiceDescriptors.OBTAIN_STRUCTURED_PUSH_CONSUMER,
            List.of());
  }

  /** Invokes SupplierAdmin::obtain_structured_pull_consumer. */
  public Ior obtainStructuredPullConsumer(Ior supplierAdmin) {
    return (Ior)
        invoke(
            supplierAdmin,
            NotificationServiceDescriptors.OBTAIN_STRUCTURED_PULL_CONSUMER,
            List.of());
  }

  /** Invokes ConsumerAdmin::obtain_structured_push_supplier. */
  public Ior obtainStructuredPushSupplier(Ior consumerAdmin) {
    return (Ior)
        invoke(
            consumerAdmin,
            NotificationServiceDescriptors.OBTAIN_STRUCTURED_PUSH_SUPPLIER,
            List.of());
  }

  /** Invokes ConsumerAdmin::obtain_structured_pull_supplier. */
  public Ior obtainStructuredPullSupplier(Ior consumerAdmin) {
    return (Ior)
        invoke(
            consumerAdmin,
            NotificationServiceDescriptors.OBTAIN_STRUCTURED_PULL_SUPPLIER,
            List.of());
  }

  /** Invokes StructuredPushConsumer::push_structured_event. */
  public void pushStructuredEvent(Ior proxyPushConsumer, NotificationStructuredEvent event) {
    invoke(proxyPushConsumer, NotificationServiceDescriptors.PUSH_STRUCTURED_EVENT, List.of(event));
  }

  /** Invokes StructuredPullSupplier::pull_structured_event. */
  public NotificationStructuredEvent pullStructuredEvent(Ior proxyPullSupplier) {
    return (NotificationStructuredEvent)
        invoke(proxyPullSupplier, NotificationServiceDescriptors.PULL_STRUCTURED_EVENT, List.of());
  }

  /** Invokes StructuredPullSupplier::try_pull_structured_event. */
  public Optional<NotificationStructuredEvent> tryPullStructuredEvent(Ior proxyPullSupplier) {
    NotificationTryPullResult result =
        (NotificationTryPullResult)
            invoke(
                proxyPullSupplier,
                NotificationServiceDescriptors.TRY_PULL_STRUCTURED_EVENT,
                List.of());
    return result.event();
  }

  /** Invokes supported local proxy filter configuration. */
  public void setFilter(Ior proxySupplier, String expression) {
    invoke(proxySupplier, NotificationServiceDescriptors.SET_FILTER, List.of(expression));
  }

  /** Invokes supported local integer QoS configuration. */
  public void setIntegerQos(Ior proxySupplier, String key, long value) {
    invoke(
        proxySupplier,
        NotificationServiceDescriptors.SET_INTEGER_QOS,
        List.of(key, Long.valueOf(value)));
  }

  /** Invokes supported local boolean QoS configuration. */
  public void setBooleanQos(Ior proxySupplier, String key, boolean value) {
    invoke(
        proxySupplier,
        NotificationServiceDescriptors.SET_BOOLEAN_QOS,
        List.of(key, Boolean.valueOf(value)));
  }

  /** Invokes StructuredPushConsumer::disconnect_structured_push_consumer. */
  public void disconnectStructuredPushConsumer(Ior proxyPushConsumer) {
    invoke(
        proxyPushConsumer,
        NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PUSH_CONSUMER,
        List.of());
  }

  /** Invokes StructuredPullConsumer::disconnect_structured_pull_consumer. */
  public void disconnectStructuredPullConsumer(Ior proxyPullConsumer) {
    invoke(
        proxyPullConsumer,
        NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PULL_CONSUMER,
        List.of());
  }

  /** Invokes StructuredPushSupplier::disconnect_structured_push_supplier. */
  public void disconnectStructuredPushSupplier(Ior proxyPushSupplier) {
    invoke(
        proxyPushSupplier,
        NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PUSH_SUPPLIER,
        List.of());
  }

  /** Invokes StructuredPullSupplier::disconnect_structured_pull_supplier. */
  public void disconnectStructuredPullSupplier(Ior proxyPullSupplier) {
    invoke(
        proxyPullSupplier,
        NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PULL_SUPPLIER,
        List.of());
  }

  /** Invokes EventChannel::destroy. */
  public void destroy(Ior eventChannel) {
    invoke(eventChannel, NotificationServiceDescriptors.DESTROY, List.of());
  }

  private Object invoke(
      Ior ior,
      io.github.mundanej.mjo.typecode.IdlOperationDescriptor operation,
      List<Object> arguments) {
    IiopObjectReference reference = IiopObjectReference.fromIor(ior);
    try (IiopOrbClient client = IiopOrbClient.connect(reference, options)) {
      return client.invoke(operation, NotificationServiceIiopCodec.INSTANCE, arguments);
    }
  }
}
