package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.iiop.IiopOperationBinding;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.List;
import java.util.Optional;

/** Static descriptors and IIOP operation bindings for the supported Event Service subset. */
public final class EventServiceDescriptors {

  /** Repository ID for CosEventChannelAdmin::EventChannel. */
  public static final RepositoryId EVENT_CHANNEL_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosEventChannelAdmin/EventChannel:1.0");

  /** Repository ID for CosEventChannelAdmin::SupplierAdmin. */
  public static final RepositoryId SUPPLIER_ADMIN_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosEventChannelAdmin/SupplierAdmin:1.0");

  /** Repository ID for CosEventChannelAdmin::ConsumerAdmin. */
  public static final RepositoryId CONSUMER_ADMIN_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosEventChannelAdmin/ConsumerAdmin:1.0");

  /** Repository ID for CosEventChannelAdmin::ProxyPushConsumer. */
  public static final RepositoryId PROXY_PUSH_CONSUMER_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosEventChannelAdmin/ProxyPushConsumer:1.0");

  /** Repository ID for CosEventChannelAdmin::ProxyPullConsumer. */
  public static final RepositoryId PROXY_PULL_CONSUMER_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosEventChannelAdmin/ProxyPullConsumer:1.0");

  /** Repository ID for CosEventChannelAdmin::ProxyPushSupplier. */
  public static final RepositoryId PROXY_PUSH_SUPPLIER_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosEventChannelAdmin/ProxyPushSupplier:1.0");

  /** Repository ID for CosEventChannelAdmin::ProxyPullSupplier. */
  public static final RepositoryId PROXY_PULL_SUPPLIER_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosEventChannelAdmin/ProxyPullSupplier:1.0");

  /** IDL void return type. */
  public static final IdlTypeReference VOID_TYPE =
      new IdlTypeReference(IdlTypeKind.VOID, "void", "void", Optional.empty());

  /** IDL boolean return type. */
  public static final IdlTypeReference BOOLEAN_TYPE = primitive("boolean", "boolean");

  /** IDL any payload type. */
  public static final IdlTypeReference ANY_TYPE =
      primitive("any", "io.github.mundanej.mjo.any.AnyValue");

  /** EventChannel object-reference type. */
  public static final IdlTypeReference EVENT_CHANNEL_TYPE =
      eventInterface(
          "::CosEventChannelAdmin::EventChannel",
          LocalEventChannel.class.getName(),
          EVENT_CHANNEL_REPOSITORY_ID);

  /** SupplierAdmin object-reference type. */
  public static final IdlTypeReference SUPPLIER_ADMIN_TYPE =
      eventInterface(
          "::CosEventChannelAdmin::SupplierAdmin",
          LocalEventSupplierAdmin.class.getName(),
          SUPPLIER_ADMIN_REPOSITORY_ID);

  /** ConsumerAdmin object-reference type. */
  public static final IdlTypeReference CONSUMER_ADMIN_TYPE =
      eventInterface(
          "::CosEventChannelAdmin::ConsumerAdmin",
          LocalEventConsumerAdmin.class.getName(),
          CONSUMER_ADMIN_REPOSITORY_ID);

  /** ProxyPushConsumer object-reference type. */
  public static final IdlTypeReference PROXY_PUSH_CONSUMER_TYPE =
      eventInterface(
          "::CosEventChannelAdmin::ProxyPushConsumer",
          LocalPushConsumerProxy.class.getName(),
          PROXY_PUSH_CONSUMER_REPOSITORY_ID);

  /** ProxyPullConsumer object-reference type. */
  public static final IdlTypeReference PROXY_PULL_CONSUMER_TYPE =
      eventInterface(
          "::CosEventChannelAdmin::ProxyPullConsumer",
          LocalPullConsumerProxy.class.getName(),
          PROXY_PULL_CONSUMER_REPOSITORY_ID);

  /** ProxyPushSupplier object-reference type. */
  public static final IdlTypeReference PROXY_PUSH_SUPPLIER_TYPE =
      eventInterface(
          "::CosEventChannelAdmin::ProxyPushSupplier",
          LocalPushSupplierProxy.class.getName(),
          PROXY_PUSH_SUPPLIER_REPOSITORY_ID);

  /** ProxyPullSupplier object-reference type. */
  public static final IdlTypeReference PROXY_PULL_SUPPLIER_TYPE =
      eventInterface(
          "::CosEventChannelAdmin::ProxyPullSupplier",
          LocalPullSupplierProxy.class.getName(),
          PROXY_PULL_SUPPLIER_REPOSITORY_ID);

  /** EventChannel::for_suppliers. */
  public static final IdlOperationDescriptor FOR_SUPPLIERS =
      new IdlOperationDescriptor("for_suppliers", SUPPLIER_ADMIN_TYPE, List.of(), List.of());

  /** EventChannel::for_consumers. */
  public static final IdlOperationDescriptor FOR_CONSUMERS =
      new IdlOperationDescriptor("for_consumers", CONSUMER_ADMIN_TYPE, List.of(), List.of());

  /** EventChannel::destroy. */
  public static final IdlOperationDescriptor DESTROY =
      new IdlOperationDescriptor("destroy", VOID_TYPE, List.of(), List.of());

  /** SupplierAdmin::obtain_push_consumer. */
  public static final IdlOperationDescriptor OBTAIN_PUSH_CONSUMER =
      new IdlOperationDescriptor(
          "obtain_push_consumer", PROXY_PUSH_CONSUMER_TYPE, List.of(), List.of());

  /** SupplierAdmin::obtain_pull_consumer. */
  public static final IdlOperationDescriptor OBTAIN_PULL_CONSUMER =
      new IdlOperationDescriptor(
          "obtain_pull_consumer", PROXY_PULL_CONSUMER_TYPE, List.of(), List.of());

  /** ConsumerAdmin::obtain_push_supplier. */
  public static final IdlOperationDescriptor OBTAIN_PUSH_SUPPLIER =
      new IdlOperationDescriptor(
          "obtain_push_supplier", PROXY_PUSH_SUPPLIER_TYPE, List.of(), List.of());

  /** ConsumerAdmin::obtain_pull_supplier. */
  public static final IdlOperationDescriptor OBTAIN_PULL_SUPPLIER =
      new IdlOperationDescriptor(
          "obtain_pull_supplier", PROXY_PULL_SUPPLIER_TYPE, List.of(), List.of());

  /** PushConsumer::push. */
  public static final IdlOperationDescriptor PUSH =
      new IdlOperationDescriptor(
          "push",
          VOID_TYPE,
          List.of(new IdlParameterDescriptor("data", IdlParameterMode.IN, ANY_TYPE)),
          List.of());

  /** PushConsumer::disconnect_push_consumer. */
  public static final IdlOperationDescriptor DISCONNECT_PUSH_CONSUMER =
      new IdlOperationDescriptor("disconnect_push_consumer", VOID_TYPE, List.of(), List.of());

  /** PullSupplier::pull. */
  public static final IdlOperationDescriptor PULL =
      new IdlOperationDescriptor("pull", ANY_TYPE, List.of(), List.of());

  /** PullSupplier::try_pull. */
  public static final IdlOperationDescriptor TRY_PULL =
      new IdlOperationDescriptor(
          "try_pull",
          ANY_TYPE,
          List.of(new IdlParameterDescriptor("has_event", IdlParameterMode.OUT, BOOLEAN_TYPE)),
          List.of());

  /** PullSupplier::disconnect_pull_supplier. */
  public static final IdlOperationDescriptor DISCONNECT_PULL_SUPPLIER =
      new IdlOperationDescriptor("disconnect_pull_supplier", VOID_TYPE, List.of(), List.of());

  /** PullConsumer::disconnect_pull_consumer. */
  public static final IdlOperationDescriptor DISCONNECT_PULL_CONSUMER =
      new IdlOperationDescriptor("disconnect_pull_consumer", VOID_TYPE, List.of(), List.of());

  /** PushSupplier::disconnect_push_supplier. */
  public static final IdlOperationDescriptor DISCONNECT_PUSH_SUPPLIER =
      new IdlOperationDescriptor("disconnect_push_supplier", VOID_TYPE, List.of(), List.of());

  /** Descriptor for EventChannel. */
  public static final IdlGeneratedTypeDescriptor EVENT_CHANNEL =
      descriptor(
          "::CosEventChannelAdmin::EventChannel",
          LocalEventChannel.class.getName(),
          EVENT_CHANNEL_REPOSITORY_ID,
          List.of(FOR_SUPPLIERS, FOR_CONSUMERS, DESTROY));

  /** Descriptor for SupplierAdmin. */
  public static final IdlGeneratedTypeDescriptor SUPPLIER_ADMIN =
      descriptor(
          "::CosEventChannelAdmin::SupplierAdmin",
          LocalEventSupplierAdmin.class.getName(),
          SUPPLIER_ADMIN_REPOSITORY_ID,
          List.of(OBTAIN_PUSH_CONSUMER, OBTAIN_PULL_CONSUMER));

  /** Descriptor for ConsumerAdmin. */
  public static final IdlGeneratedTypeDescriptor CONSUMER_ADMIN =
      descriptor(
          "::CosEventChannelAdmin::ConsumerAdmin",
          LocalEventConsumerAdmin.class.getName(),
          CONSUMER_ADMIN_REPOSITORY_ID,
          List.of(OBTAIN_PUSH_SUPPLIER, OBTAIN_PULL_SUPPLIER));

  /** Descriptor for ProxyPushConsumer. */
  public static final IdlGeneratedTypeDescriptor PROXY_PUSH_CONSUMER =
      descriptor(
          "::CosEventChannelAdmin::ProxyPushConsumer",
          LocalPushConsumerProxy.class.getName(),
          PROXY_PUSH_CONSUMER_REPOSITORY_ID,
          List.of(PUSH, DISCONNECT_PUSH_CONSUMER));

  /** Descriptor for ProxyPullConsumer. */
  public static final IdlGeneratedTypeDescriptor PROXY_PULL_CONSUMER =
      descriptor(
          "::CosEventChannelAdmin::ProxyPullConsumer",
          LocalPullConsumerProxy.class.getName(),
          PROXY_PULL_CONSUMER_REPOSITORY_ID,
          List.of(DISCONNECT_PULL_CONSUMER));

  /** Descriptor for ProxyPushSupplier. */
  public static final IdlGeneratedTypeDescriptor PROXY_PUSH_SUPPLIER =
      descriptor(
          "::CosEventChannelAdmin::ProxyPushSupplier",
          LocalPushSupplierProxy.class.getName(),
          PROXY_PUSH_SUPPLIER_REPOSITORY_ID,
          List.of(DISCONNECT_PUSH_SUPPLIER));

  /** Descriptor for ProxyPullSupplier. */
  public static final IdlGeneratedTypeDescriptor PROXY_PULL_SUPPLIER =
      descriptor(
          "::CosEventChannelAdmin::ProxyPullSupplier",
          LocalPullSupplierProxy.class.getName(),
          PROXY_PULL_SUPPLIER_REPOSITORY_ID,
          List.of(PULL, TRY_PULL, DISCONNECT_PULL_SUPPLIER));

  private static final List<IiopOperationBinding> EVENT_CHANNEL_BINDINGS =
      bindings(FOR_SUPPLIERS, FOR_CONSUMERS, DESTROY);
  private static final List<IiopOperationBinding> SUPPLIER_ADMIN_BINDINGS =
      bindings(OBTAIN_PUSH_CONSUMER, OBTAIN_PULL_CONSUMER);
  private static final List<IiopOperationBinding> CONSUMER_ADMIN_BINDINGS =
      bindings(OBTAIN_PUSH_SUPPLIER, OBTAIN_PULL_SUPPLIER);
  private static final List<IiopOperationBinding> PROXY_PUSH_CONSUMER_BINDINGS =
      bindings(PUSH, DISCONNECT_PUSH_CONSUMER);
  private static final List<IiopOperationBinding> PROXY_PULL_CONSUMER_BINDINGS =
      bindings(DISCONNECT_PULL_CONSUMER);
  private static final List<IiopOperationBinding> PROXY_PUSH_SUPPLIER_BINDINGS =
      bindings(DISCONNECT_PUSH_SUPPLIER);
  private static final List<IiopOperationBinding> PROXY_PULL_SUPPLIER_BINDINGS =
      bindings(PULL, TRY_PULL, DISCONNECT_PULL_SUPPLIER);

  private EventServiceDescriptors() {}

  /** Returns EventChannel IIOP operation bindings. */
  public static List<IiopOperationBinding> eventChannelBindings() {
    return List.copyOf(EVENT_CHANNEL_BINDINGS);
  }

  /** Returns SupplierAdmin IIOP operation bindings. */
  public static List<IiopOperationBinding> supplierAdminBindings() {
    return List.copyOf(SUPPLIER_ADMIN_BINDINGS);
  }

  /** Returns ConsumerAdmin IIOP operation bindings. */
  public static List<IiopOperationBinding> consumerAdminBindings() {
    return List.copyOf(CONSUMER_ADMIN_BINDINGS);
  }

  /** Returns ProxyPushConsumer IIOP operation bindings. */
  public static List<IiopOperationBinding> proxyPushConsumerBindings() {
    return List.copyOf(PROXY_PUSH_CONSUMER_BINDINGS);
  }

  /** Returns ProxyPullConsumer IIOP operation bindings. */
  public static List<IiopOperationBinding> proxyPullConsumerBindings() {
    return List.copyOf(PROXY_PULL_CONSUMER_BINDINGS);
  }

  /** Returns ProxyPushSupplier IIOP operation bindings. */
  public static List<IiopOperationBinding> proxyPushSupplierBindings() {
    return List.copyOf(PROXY_PUSH_SUPPLIER_BINDINGS);
  }

  /** Returns ProxyPullSupplier IIOP operation bindings. */
  public static List<IiopOperationBinding> proxyPullSupplierBindings() {
    return List.copyOf(PROXY_PULL_SUPPLIER_BINDINGS);
  }

  private static IdlGeneratedTypeDescriptor descriptor(
      String idlName,
      String javaName,
      RepositoryId repositoryId,
      List<IdlOperationDescriptor> operations) {
    return new IdlGeneratedTypeDescriptor(
        IdlTypeKind.INTERFACE, idlName, javaName, repositoryId, List.of(), List.of(), operations);
  }

  private static IdlTypeReference eventInterface(
      String idlName, String javaName, RepositoryId repositoryId) {
    return new IdlTypeReference(
        IdlTypeKind.INTERFACE, idlName, javaName, Optional.of(repositoryId));
  }

  private static IdlTypeReference primitive(String idlName, String javaName) {
    return new IdlTypeReference(IdlTypeKind.PRIMITIVE, idlName, javaName, Optional.empty());
  }

  private static List<IiopOperationBinding> bindings(IdlOperationDescriptor... operations) {
    return java.util.Arrays.stream(operations)
        .map(operation -> new IiopOperationBinding(operation, EventServiceIiopCodec.INSTANCE))
        .toList();
  }
}
