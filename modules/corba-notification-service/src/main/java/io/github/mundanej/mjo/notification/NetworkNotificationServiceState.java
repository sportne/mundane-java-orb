package io.github.mundanej.mjo.notification;

import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.ObjectKey;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.omg.CORBA.CompletionStatus;

final class NetworkNotificationServiceState {

  private final LocalOrb orb;
  private final LocalNotificationChannel channel;
  private final Map<ObjectKey, LocalObjectReference<?>> references = new LinkedHashMap<>();
  private LocalObjectReference<LocalNotificationChannel> channelReference;
  private LocalObjectReference<LocalNotificationSupplierAdmin> supplierAdminReference;
  private LocalObjectReference<LocalNotificationConsumerAdmin> consumerAdminReference;
  private io.github.mundanej.mjo.iiop.IiopEndpoint endpoint;

  NetworkNotificationServiceState(LocalOrb orb, LocalNotificationChannel channel) {
    this.orb = Objects.requireNonNull(orb, "orb");
    this.channel = Objects.requireNonNull(channel, "channel");
    bindInitialReferences();
  }

  LocalOrb orb() {
    return orb;
  }

  LocalNotificationChannel channel() {
    return channel;
  }

  LocalObjectReference<LocalNotificationChannel> channelReference() {
    return channelReference;
  }

  LocalObjectReference<LocalNotificationSupplierAdmin> supplierAdminReference() {
    return supplierAdminReference;
  }

  LocalObjectReference<LocalNotificationConsumerAdmin> consumerAdminReference() {
    return consumerAdminReference;
  }

  synchronized void endpoint(io.github.mundanej.mjo.iiop.IiopEndpoint endpoint) {
    this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
  }

  synchronized Ior bindProxy(LocalNotificationProxy proxy) {
    if (proxy instanceof LocalStructuredPushConsumerProxy typed) {
      return ior(
          bind(
              LocalStructuredPushConsumerProxy.class,
              NotificationServiceDescriptors.STRUCTURED_PUSH_CONSUMER,
              objectId(proxy),
              typed));
    }
    if (proxy instanceof LocalStructuredPullConsumerProxy typed) {
      return ior(
          bind(
              LocalStructuredPullConsumerProxy.class,
              NotificationServiceDescriptors.STRUCTURED_PULL_CONSUMER,
              objectId(proxy),
              typed));
    }
    if (proxy instanceof LocalStructuredPushSupplierProxy typed) {
      return ior(
          bind(
              LocalStructuredPushSupplierProxy.class,
              NotificationServiceDescriptors.STRUCTURED_PUSH_SUPPLIER,
              objectId(proxy),
              typed));
    }
    if (proxy instanceof LocalStructuredPullSupplierProxy typed) {
      return ior(
          bind(
              LocalStructuredPullSupplierProxy.class,
              NotificationServiceDescriptors.STRUCTURED_PULL_SUPPLIER,
              objectId(proxy),
              typed));
    }
    throw NotificationServiceCorbaExceptions.badParam(
        "Unsupported Notification Service proxy: " + proxy.kind());
  }

  synchronized Ior ior(LocalObjectReference<?> reference) {
    if (endpoint == null) {
      throw NotificationServiceCorbaExceptions.badParam(
          "Notification Service endpoint has not been assigned");
    }
    return IiopObjectReference.fromLocal(endpoint, reference).ior();
  }

  synchronized LocalObjectReference<?> resolve(byte[] objectKey) {
    LocalObjectReference<?> reference = references.get(new ObjectKey(objectKey));
    if (reference == null) {
      throw new org.omg.CORBA.OBJECT_NOT_EXIST(
          "Unknown Notification Service object key", 0, CompletionStatus.COMPLETED_NO);
    }
    return reference;
  }

  private void bindInitialReferences() {
    channelReference =
        bind(
            LocalNotificationChannel.class,
            NotificationServiceDescriptors.EVENT_CHANNEL,
            NetworkNotificationService.DEFAULT_OBJECT_ID,
            channel);
    supplierAdminReference =
        bind(
            LocalNotificationSupplierAdmin.class,
            NotificationServiceDescriptors.SUPPLIER_ADMIN,
            "NotificationSupplierAdmin-" + channel.id(),
            channel.supplierAdmin());
    consumerAdminReference =
        bind(
            LocalNotificationConsumerAdmin.class,
            NotificationServiceDescriptors.CONSUMER_ADMIN,
            "NotificationConsumerAdmin-" + channel.id(),
            channel.consumerAdmin());
  }

  private <T> LocalObjectReference<T> bind(
      Class<T> javaType, IdlGeneratedTypeDescriptor descriptor, String objectId, T target) {
    LocalObjectReference<T> reference =
        orb.bindWithObjectId(
            javaType, descriptor, objectId, new NotificationServiceDispatcher(this, target));
    references.put(new ObjectKey(objectId.getBytes(StandardCharsets.US_ASCII)), reference);
    return reference;
  }

  private static String objectId(LocalNotificationProxy proxy) {
    return "NotificationProxy-" + proxy.kind().name() + "-" + proxy.channelId() + "-" + proxy.id();
  }
}
