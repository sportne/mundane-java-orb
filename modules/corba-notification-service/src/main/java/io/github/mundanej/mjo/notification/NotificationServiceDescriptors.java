package io.github.mundanej.mjo.notification;

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

/** Static descriptors and IIOP bindings for the supported Notification Service subset. */
public final class NotificationServiceDescriptors {

  /** Repository ID for CosNotifyChannelAdmin::EventChannel. */
  public static final RepositoryId EVENT_CHANNEL_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosNotifyChannelAdmin/EventChannel:1.0");

  /** Repository ID for CosNotifyChannelAdmin::SupplierAdmin. */
  public static final RepositoryId SUPPLIER_ADMIN_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosNotifyChannelAdmin/SupplierAdmin:1.0");

  /** Repository ID for CosNotifyChannelAdmin::ConsumerAdmin. */
  public static final RepositoryId CONSUMER_ADMIN_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosNotifyChannelAdmin/ConsumerAdmin:1.0");

  /** Repository ID for CosNotifyChannelAdmin::StructuredProxyPushConsumer. */
  public static final RepositoryId STRUCTURED_PUSH_CONSUMER_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosNotifyChannelAdmin/StructuredProxyPushConsumer:1.0");

  /** Repository ID for CosNotifyChannelAdmin::StructuredProxyPullConsumer. */
  public static final RepositoryId STRUCTURED_PULL_CONSUMER_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosNotifyChannelAdmin/StructuredProxyPullConsumer:1.0");

  /** Repository ID for CosNotifyChannelAdmin::StructuredProxyPushSupplier. */
  public static final RepositoryId STRUCTURED_PUSH_SUPPLIER_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosNotifyChannelAdmin/StructuredProxyPushSupplier:1.0");

  /** Repository ID for CosNotifyChannelAdmin::StructuredProxyPullSupplier. */
  public static final RepositoryId STRUCTURED_PULL_SUPPLIER_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosNotifyChannelAdmin/StructuredProxyPullSupplier:1.0");

  /** IDL void return type. */
  public static final IdlTypeReference VOID_TYPE =
      new IdlTypeReference(IdlTypeKind.VOID, "void", "void", Optional.empty());

  /** IDL boolean type. */
  public static final IdlTypeReference BOOLEAN_TYPE = primitive("boolean", "boolean");

  /** IDL string type. */
  public static final IdlTypeReference STRING_TYPE = primitive("string", String.class.getName());

  /** IDL signed long long type. */
  public static final IdlTypeReference LONG_LONG_TYPE = primitive("long long", "long");

  /** Supported structured-event value type. */
  public static final IdlTypeReference STRUCTURED_EVENT_TYPE =
      new IdlTypeReference(
          IdlTypeKind.STRUCT,
          "::CosNotification::StructuredEvent",
          NotificationStructuredEvent.class.getName(),
          Optional.empty());

  /** Notification EventChannel object-reference type. */
  public static final IdlTypeReference EVENT_CHANNEL_TYPE =
      notificationInterface(
          "::CosNotifyChannelAdmin::EventChannel",
          LocalNotificationChannel.class.getName(),
          EVENT_CHANNEL_REPOSITORY_ID);

  /** Notification SupplierAdmin object-reference type. */
  public static final IdlTypeReference SUPPLIER_ADMIN_TYPE =
      notificationInterface(
          "::CosNotifyChannelAdmin::SupplierAdmin",
          LocalNotificationSupplierAdmin.class.getName(),
          SUPPLIER_ADMIN_REPOSITORY_ID);

  /** Notification ConsumerAdmin object-reference type. */
  public static final IdlTypeReference CONSUMER_ADMIN_TYPE =
      notificationInterface(
          "::CosNotifyChannelAdmin::ConsumerAdmin",
          LocalNotificationConsumerAdmin.class.getName(),
          CONSUMER_ADMIN_REPOSITORY_ID);

  /** StructuredProxyPushConsumer object-reference type. */
  public static final IdlTypeReference STRUCTURED_PUSH_CONSUMER_TYPE =
      notificationInterface(
          "::CosNotifyChannelAdmin::StructuredProxyPushConsumer",
          LocalStructuredPushConsumerProxy.class.getName(),
          STRUCTURED_PUSH_CONSUMER_REPOSITORY_ID);

  /** StructuredProxyPullConsumer object-reference type. */
  public static final IdlTypeReference STRUCTURED_PULL_CONSUMER_TYPE =
      notificationInterface(
          "::CosNotifyChannelAdmin::StructuredProxyPullConsumer",
          LocalStructuredPullConsumerProxy.class.getName(),
          STRUCTURED_PULL_CONSUMER_REPOSITORY_ID);

  /** StructuredProxyPushSupplier object-reference type. */
  public static final IdlTypeReference STRUCTURED_PUSH_SUPPLIER_TYPE =
      notificationInterface(
          "::CosNotifyChannelAdmin::StructuredProxyPushSupplier",
          LocalStructuredPushSupplierProxy.class.getName(),
          STRUCTURED_PUSH_SUPPLIER_REPOSITORY_ID);

  /** StructuredProxyPullSupplier object-reference type. */
  public static final IdlTypeReference STRUCTURED_PULL_SUPPLIER_TYPE =
      notificationInterface(
          "::CosNotifyChannelAdmin::StructuredProxyPullSupplier",
          LocalStructuredPullSupplierProxy.class.getName(),
          STRUCTURED_PULL_SUPPLIER_REPOSITORY_ID);

  /** EventChannel::for_suppliers. */
  public static final IdlOperationDescriptor FOR_SUPPLIERS =
      new IdlOperationDescriptor("for_suppliers", SUPPLIER_ADMIN_TYPE, List.of(), List.of());

  /** EventChannel::for_consumers. */
  public static final IdlOperationDescriptor FOR_CONSUMERS =
      new IdlOperationDescriptor("for_consumers", CONSUMER_ADMIN_TYPE, List.of(), List.of());

  /** EventChannel::destroy. */
  public static final IdlOperationDescriptor DESTROY =
      new IdlOperationDescriptor("destroy", VOID_TYPE, List.of(), List.of());

  /** SupplierAdmin::obtain_structured_push_consumer. */
  public static final IdlOperationDescriptor OBTAIN_STRUCTURED_PUSH_CONSUMER =
      new IdlOperationDescriptor(
          "obtain_structured_push_consumer", STRUCTURED_PUSH_CONSUMER_TYPE, List.of(), List.of());

  /** SupplierAdmin::obtain_structured_pull_consumer. */
  public static final IdlOperationDescriptor OBTAIN_STRUCTURED_PULL_CONSUMER =
      new IdlOperationDescriptor(
          "obtain_structured_pull_consumer", STRUCTURED_PULL_CONSUMER_TYPE, List.of(), List.of());

  /** ConsumerAdmin::obtain_structured_push_supplier. */
  public static final IdlOperationDescriptor OBTAIN_STRUCTURED_PUSH_SUPPLIER =
      new IdlOperationDescriptor(
          "obtain_structured_push_supplier", STRUCTURED_PUSH_SUPPLIER_TYPE, List.of(), List.of());

  /** ConsumerAdmin::obtain_structured_pull_supplier. */
  public static final IdlOperationDescriptor OBTAIN_STRUCTURED_PULL_SUPPLIER =
      new IdlOperationDescriptor(
          "obtain_structured_pull_supplier", STRUCTURED_PULL_SUPPLIER_TYPE, List.of(), List.of());

  /** StructuredPushConsumer::push_structured_event. */
  public static final IdlOperationDescriptor PUSH_STRUCTURED_EVENT =
      new IdlOperationDescriptor(
          "push_structured_event",
          VOID_TYPE,
          List.of(new IdlParameterDescriptor("event", IdlParameterMode.IN, STRUCTURED_EVENT_TYPE)),
          List.of());

  /** StructuredPushConsumer::disconnect_structured_push_consumer. */
  public static final IdlOperationDescriptor DISCONNECT_STRUCTURED_PUSH_CONSUMER =
      new IdlOperationDescriptor(
          "disconnect_structured_push_consumer", VOID_TYPE, List.of(), List.of());

  /** StructuredPullConsumer::disconnect_structured_pull_consumer. */
  public static final IdlOperationDescriptor DISCONNECT_STRUCTURED_PULL_CONSUMER =
      new IdlOperationDescriptor(
          "disconnect_structured_pull_consumer", VOID_TYPE, List.of(), List.of());

  /** StructuredPushSupplier::disconnect_structured_push_supplier. */
  public static final IdlOperationDescriptor DISCONNECT_STRUCTURED_PUSH_SUPPLIER =
      new IdlOperationDescriptor(
          "disconnect_structured_push_supplier", VOID_TYPE, List.of(), List.of());

  /** StructuredPullSupplier::pull_structured_event. */
  public static final IdlOperationDescriptor PULL_STRUCTURED_EVENT =
      new IdlOperationDescriptor(
          "pull_structured_event", STRUCTURED_EVENT_TYPE, List.of(), List.of());

  /** StructuredPullSupplier::try_pull_structured_event. */
  public static final IdlOperationDescriptor TRY_PULL_STRUCTURED_EVENT =
      new IdlOperationDescriptor(
          "try_pull_structured_event",
          STRUCTURED_EVENT_TYPE,
          List.of(new IdlParameterDescriptor("has_event", IdlParameterMode.OUT, BOOLEAN_TYPE)),
          List.of());

  /** StructuredPullSupplier::disconnect_structured_pull_supplier. */
  public static final IdlOperationDescriptor DISCONNECT_STRUCTURED_PULL_SUPPLIER =
      new IdlOperationDescriptor(
          "disconnect_structured_pull_supplier", VOID_TYPE, List.of(), List.of());

  /** Supported local proxy filter configuration. */
  public static final IdlOperationDescriptor SET_FILTER =
      new IdlOperationDescriptor(
          "set_filter",
          VOID_TYPE,
          List.of(new IdlParameterDescriptor("expression", IdlParameterMode.IN, STRING_TYPE)),
          List.of());

  /** Supported local integer policy configuration. */
  public static final IdlOperationDescriptor SET_INTEGER_QOS =
      new IdlOperationDescriptor(
          "set_integer_qos",
          VOID_TYPE,
          List.of(
              new IdlParameterDescriptor("key", IdlParameterMode.IN, STRING_TYPE),
              new IdlParameterDescriptor("value", IdlParameterMode.IN, LONG_LONG_TYPE)),
          List.of());

  /** Supported local boolean policy configuration. */
  public static final IdlOperationDescriptor SET_BOOLEAN_QOS =
      new IdlOperationDescriptor(
          "set_boolean_qos",
          VOID_TYPE,
          List.of(
              new IdlParameterDescriptor("key", IdlParameterMode.IN, STRING_TYPE),
              new IdlParameterDescriptor("value", IdlParameterMode.IN, BOOLEAN_TYPE)),
          List.of());

  /** Descriptor for Notification EventChannel. */
  public static final IdlGeneratedTypeDescriptor EVENT_CHANNEL =
      descriptor(
          "::CosNotifyChannelAdmin::EventChannel",
          LocalNotificationChannel.class.getName(),
          EVENT_CHANNEL_REPOSITORY_ID,
          List.of(FOR_SUPPLIERS, FOR_CONSUMERS, DESTROY));

  /** Descriptor for Notification SupplierAdmin. */
  public static final IdlGeneratedTypeDescriptor SUPPLIER_ADMIN =
      descriptor(
          "::CosNotifyChannelAdmin::SupplierAdmin",
          LocalNotificationSupplierAdmin.class.getName(),
          SUPPLIER_ADMIN_REPOSITORY_ID,
          List.of(OBTAIN_STRUCTURED_PUSH_CONSUMER, OBTAIN_STRUCTURED_PULL_CONSUMER));

  /** Descriptor for Notification ConsumerAdmin. */
  public static final IdlGeneratedTypeDescriptor CONSUMER_ADMIN =
      descriptor(
          "::CosNotifyChannelAdmin::ConsumerAdmin",
          LocalNotificationConsumerAdmin.class.getName(),
          CONSUMER_ADMIN_REPOSITORY_ID,
          List.of(OBTAIN_STRUCTURED_PUSH_SUPPLIER, OBTAIN_STRUCTURED_PULL_SUPPLIER));

  /** Descriptor for StructuredProxyPushConsumer. */
  public static final IdlGeneratedTypeDescriptor STRUCTURED_PUSH_CONSUMER =
      descriptor(
          "::CosNotifyChannelAdmin::StructuredProxyPushConsumer",
          LocalStructuredPushConsumerProxy.class.getName(),
          STRUCTURED_PUSH_CONSUMER_REPOSITORY_ID,
          List.of(PUSH_STRUCTURED_EVENT, DISCONNECT_STRUCTURED_PUSH_CONSUMER));

  /** Descriptor for StructuredProxyPullConsumer. */
  public static final IdlGeneratedTypeDescriptor STRUCTURED_PULL_CONSUMER =
      descriptor(
          "::CosNotifyChannelAdmin::StructuredProxyPullConsumer",
          LocalStructuredPullConsumerProxy.class.getName(),
          STRUCTURED_PULL_CONSUMER_REPOSITORY_ID,
          List.of(DISCONNECT_STRUCTURED_PULL_CONSUMER));

  /** Descriptor for StructuredProxyPushSupplier. */
  public static final IdlGeneratedTypeDescriptor STRUCTURED_PUSH_SUPPLIER =
      descriptor(
          "::CosNotifyChannelAdmin::StructuredProxyPushSupplier",
          LocalStructuredPushSupplierProxy.class.getName(),
          STRUCTURED_PUSH_SUPPLIER_REPOSITORY_ID,
          List.of(
              DISCONNECT_STRUCTURED_PUSH_SUPPLIER, SET_FILTER, SET_INTEGER_QOS, SET_BOOLEAN_QOS));

  /** Descriptor for StructuredProxyPullSupplier. */
  public static final IdlGeneratedTypeDescriptor STRUCTURED_PULL_SUPPLIER =
      descriptor(
          "::CosNotifyChannelAdmin::StructuredProxyPullSupplier",
          LocalStructuredPullSupplierProxy.class.getName(),
          STRUCTURED_PULL_SUPPLIER_REPOSITORY_ID,
          List.of(
              PULL_STRUCTURED_EVENT,
              TRY_PULL_STRUCTURED_EVENT,
              DISCONNECT_STRUCTURED_PULL_SUPPLIER,
              SET_FILTER,
              SET_INTEGER_QOS,
              SET_BOOLEAN_QOS));

  private static final List<IiopOperationBinding> EVENT_CHANNEL_BINDINGS =
      bindings(FOR_SUPPLIERS, FOR_CONSUMERS, DESTROY);
  private static final List<IiopOperationBinding> SUPPLIER_ADMIN_BINDINGS =
      bindings(OBTAIN_STRUCTURED_PUSH_CONSUMER, OBTAIN_STRUCTURED_PULL_CONSUMER);
  private static final List<IiopOperationBinding> CONSUMER_ADMIN_BINDINGS =
      bindings(OBTAIN_STRUCTURED_PUSH_SUPPLIER, OBTAIN_STRUCTURED_PULL_SUPPLIER);
  private static final List<IiopOperationBinding> STRUCTURED_PUSH_CONSUMER_BINDINGS =
      bindings(PUSH_STRUCTURED_EVENT, DISCONNECT_STRUCTURED_PUSH_CONSUMER);
  private static final List<IiopOperationBinding> STRUCTURED_PULL_CONSUMER_BINDINGS =
      bindings(DISCONNECT_STRUCTURED_PULL_CONSUMER);
  private static final List<IiopOperationBinding> STRUCTURED_PUSH_SUPPLIER_BINDINGS =
      bindings(DISCONNECT_STRUCTURED_PUSH_SUPPLIER, SET_FILTER, SET_INTEGER_QOS, SET_BOOLEAN_QOS);
  private static final List<IiopOperationBinding> STRUCTURED_PULL_SUPPLIER_BINDINGS =
      bindings(
          PULL_STRUCTURED_EVENT,
          TRY_PULL_STRUCTURED_EVENT,
          DISCONNECT_STRUCTURED_PULL_SUPPLIER,
          SET_FILTER,
          SET_INTEGER_QOS,
          SET_BOOLEAN_QOS);

  private NotificationServiceDescriptors() {}

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

  /** Returns StructuredProxyPushConsumer IIOP operation bindings. */
  public static List<IiopOperationBinding> structuredPushConsumerBindings() {
    return List.copyOf(STRUCTURED_PUSH_CONSUMER_BINDINGS);
  }

  /** Returns StructuredProxyPullConsumer IIOP operation bindings. */
  public static List<IiopOperationBinding> structuredPullConsumerBindings() {
    return List.copyOf(STRUCTURED_PULL_CONSUMER_BINDINGS);
  }

  /** Returns StructuredProxyPushSupplier IIOP operation bindings. */
  public static List<IiopOperationBinding> structuredPushSupplierBindings() {
    return List.copyOf(STRUCTURED_PUSH_SUPPLIER_BINDINGS);
  }

  /** Returns StructuredProxyPullSupplier IIOP operation bindings. */
  public static List<IiopOperationBinding> structuredPullSupplierBindings() {
    return List.copyOf(STRUCTURED_PULL_SUPPLIER_BINDINGS);
  }

  private static IdlGeneratedTypeDescriptor descriptor(
      String idlName,
      String javaName,
      RepositoryId repositoryId,
      List<IdlOperationDescriptor> operations) {
    return new IdlGeneratedTypeDescriptor(
        IdlTypeKind.INTERFACE, idlName, javaName, repositoryId, List.of(), List.of(), operations);
  }

  private static IdlTypeReference notificationInterface(
      String idlName, String javaName, RepositoryId repositoryId) {
    return new IdlTypeReference(
        IdlTypeKind.INTERFACE, idlName, javaName, Optional.of(repositoryId));
  }

  private static IdlTypeReference primitive(String idlName, String javaName) {
    return new IdlTypeReference(IdlTypeKind.PRIMITIVE, idlName, javaName, Optional.empty());
  }

  private static List<IiopOperationBinding> bindings(IdlOperationDescriptor... operations) {
    return java.util.Arrays.stream(operations)
        .map(
            operation -> new IiopOperationBinding(operation, NotificationServiceIiopCodec.INSTANCE))
        .toList();
  }
}
