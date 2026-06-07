package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopOrbClient;
import io.github.mundanej.mjo.ior.Ior;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Client helper for invoking the supported Event Service IIOP operations. */
public final class NetworkEventServiceClient {

  private final IiopOptions options;

  private NetworkEventServiceClient(IiopOptions options) {
    this.options = Objects.requireNonNull(options, "options");
  }

  /** Creates a client helper with explicit IIOP options. */
  public static NetworkEventServiceClient create(IiopOptions options) {
    return new NetworkEventServiceClient(options);
  }

  /** Invokes EventChannel::for_suppliers. */
  public Ior forSuppliers(Ior eventChannel) {
    return (Ior) invoke(eventChannel, EventServiceDescriptors.FOR_SUPPLIERS, List.of());
  }

  /** Invokes EventChannel::for_consumers. */
  public Ior forConsumers(Ior eventChannel) {
    return (Ior) invoke(eventChannel, EventServiceDescriptors.FOR_CONSUMERS, List.of());
  }

  /** Invokes SupplierAdmin::obtain_push_consumer. */
  public Ior obtainPushConsumer(Ior supplierAdmin) {
    return (Ior) invoke(supplierAdmin, EventServiceDescriptors.OBTAIN_PUSH_CONSUMER, List.of());
  }

  /** Invokes SupplierAdmin::obtain_pull_consumer. */
  public Ior obtainPullConsumer(Ior supplierAdmin) {
    return (Ior) invoke(supplierAdmin, EventServiceDescriptors.OBTAIN_PULL_CONSUMER, List.of());
  }

  /** Invokes ConsumerAdmin::obtain_push_supplier. */
  public Ior obtainPushSupplier(Ior consumerAdmin) {
    return (Ior) invoke(consumerAdmin, EventServiceDescriptors.OBTAIN_PUSH_SUPPLIER, List.of());
  }

  /** Invokes ConsumerAdmin::obtain_pull_supplier. */
  public Ior obtainPullSupplier(Ior consumerAdmin) {
    return (Ior) invoke(consumerAdmin, EventServiceDescriptors.OBTAIN_PULL_SUPPLIER, List.of());
  }

  /** Invokes ProxyPushConsumer::push. */
  public void push(Ior proxyPushConsumer, AnyValue<?> event) {
    invoke(proxyPushConsumer, EventServiceDescriptors.PUSH, List.of(event));
  }

  /** Invokes ProxyPullSupplier::pull. */
  public AnyValue<?> pull(Ior proxyPullSupplier) {
    return (AnyValue<?>) invoke(proxyPullSupplier, EventServiceDescriptors.PULL, List.of());
  }

  /** Invokes ProxyPullSupplier::try_pull. */
  public Optional<AnyValue<?>> tryPull(Ior proxyPullSupplier) {
    EventTryPullResult result =
        (EventTryPullResult) invoke(proxyPullSupplier, EventServiceDescriptors.TRY_PULL, List.of());
    return result.event();
  }

  /** Invokes ProxyPushConsumer::disconnect_push_consumer. */
  public void disconnectPushConsumer(Ior proxyPushConsumer) {
    invoke(proxyPushConsumer, EventServiceDescriptors.DISCONNECT_PUSH_CONSUMER, List.of());
  }

  /** Invokes ProxyPullConsumer::disconnect_pull_consumer. */
  public void disconnectPullConsumer(Ior proxyPullConsumer) {
    invoke(proxyPullConsumer, EventServiceDescriptors.DISCONNECT_PULL_CONSUMER, List.of());
  }

  /** Invokes ProxyPushSupplier::disconnect_push_supplier. */
  public void disconnectPushSupplier(Ior proxyPushSupplier) {
    invoke(proxyPushSupplier, EventServiceDescriptors.DISCONNECT_PUSH_SUPPLIER, List.of());
  }

  /** Invokes ProxyPullSupplier::disconnect_pull_supplier. */
  public void disconnectPullSupplier(Ior proxyPullSupplier) {
    invoke(proxyPullSupplier, EventServiceDescriptors.DISCONNECT_PULL_SUPPLIER, List.of());
  }

  /** Invokes EventChannel::destroy. */
  public void destroy(Ior eventChannel) {
    invoke(eventChannel, EventServiceDescriptors.DESTROY, List.of());
  }

  private Object invoke(
      Ior ior,
      io.github.mundanej.mjo.typecode.IdlOperationDescriptor operation,
      List<Object> arguments) {
    IiopObjectReference reference = IiopObjectReference.fromIor(ior);
    try (IiopOrbClient client = IiopOrbClient.connect(reference, options)) {
      return client.invoke(operation, EventServiceIiopCodec.INSTANCE, arguments);
    }
  }
}
