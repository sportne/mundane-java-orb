package io.github.mundanej.mjo.event;

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

final class NetworkEventServiceState {

  private final LocalOrb orb;
  private final LocalEventChannel channel;
  private final Map<ObjectKey, LocalObjectReference<?>> references = new LinkedHashMap<>();
  private LocalObjectReference<LocalEventChannel> channelReference;
  private LocalObjectReference<LocalEventSupplierAdmin> supplierAdminReference;
  private LocalObjectReference<LocalEventConsumerAdmin> consumerAdminReference;
  private io.github.mundanej.mjo.iiop.IiopEndpoint endpoint;

  NetworkEventServiceState(LocalOrb orb, LocalEventChannel channel) {
    this.orb = Objects.requireNonNull(orb, "orb");
    this.channel = Objects.requireNonNull(channel, "channel");
    bindInitialReferences();
  }

  LocalOrb orb() {
    return orb;
  }

  LocalEventChannel channel() {
    return channel;
  }

  LocalObjectReference<LocalEventChannel> channelReference() {
    return channelReference;
  }

  LocalObjectReference<LocalEventSupplierAdmin> supplierAdminReference() {
    return supplierAdminReference;
  }

  LocalObjectReference<LocalEventConsumerAdmin> consumerAdminReference() {
    return consumerAdminReference;
  }

  synchronized void endpoint(io.github.mundanej.mjo.iiop.IiopEndpoint endpoint) {
    this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
  }

  synchronized Ior bindProxy(LocalEventProxy proxy) {
    if (proxy instanceof LocalPushConsumerProxy typed) {
      return ior(
          bind(
              LocalPushConsumerProxy.class,
              EventServiceDescriptors.PROXY_PUSH_CONSUMER,
              objectId(proxy),
              typed));
    }
    if (proxy instanceof LocalPullConsumerProxy typed) {
      return ior(
          bind(
              LocalPullConsumerProxy.class,
              EventServiceDescriptors.PROXY_PULL_CONSUMER,
              objectId(proxy),
              typed));
    }
    if (proxy instanceof LocalPushSupplierProxy typed) {
      return ior(
          bind(
              LocalPushSupplierProxy.class,
              EventServiceDescriptors.PROXY_PUSH_SUPPLIER,
              objectId(proxy),
              typed));
    }
    if (proxy instanceof LocalPullSupplierProxy typed) {
      return ior(
          bind(
              LocalPullSupplierProxy.class,
              EventServiceDescriptors.PROXY_PULL_SUPPLIER,
              objectId(proxy),
              typed));
    }
    throw EventServiceCorbaExceptions.badParam("Unsupported Event Service proxy: " + proxy.kind());
  }

  synchronized Ior ior(LocalObjectReference<?> reference) {
    if (endpoint == null) {
      throw EventServiceCorbaExceptions.badParam("Event Service endpoint has not been assigned");
    }
    return IiopObjectReference.fromLocal(endpoint, reference).ior();
  }

  synchronized LocalObjectReference<?> resolve(byte[] objectKey) {
    LocalObjectReference<?> reference = references.get(new ObjectKey(objectKey));
    if (reference == null) {
      throw new org.omg.CORBA.OBJECT_NOT_EXIST(
          "Unknown Event Service object key", 0, CompletionStatus.COMPLETED_NO);
    }
    return reference;
  }

  private void bindInitialReferences() {
    channelReference =
        bind(
            LocalEventChannel.class,
            EventServiceDescriptors.EVENT_CHANNEL,
            NetworkEventService.DEFAULT_OBJECT_ID,
            channel);
    supplierAdminReference =
        bind(
            LocalEventSupplierAdmin.class,
            EventServiceDescriptors.SUPPLIER_ADMIN,
            "EventSupplierAdmin-" + channel.id(),
            channel.supplierAdmin());
    consumerAdminReference =
        bind(
            LocalEventConsumerAdmin.class,
            EventServiceDescriptors.CONSUMER_ADMIN,
            "EventConsumerAdmin-" + channel.id(),
            channel.consumerAdmin());
  }

  private <T> LocalObjectReference<T> bind(
      Class<T> javaType, IdlGeneratedTypeDescriptor descriptor, String objectId, T target) {
    LocalObjectReference<T> reference =
        orb.bindWithObjectId(
            javaType, descriptor, objectId, new EventServiceDispatcher(this, target));
    references.put(new ObjectKey(objectId.getBytes(StandardCharsets.US_ASCII)), reference);
    return reference;
  }

  private static String objectId(LocalEventProxy proxy) {
    return "EventProxy-" + proxy.kind().name() + "-" + proxy.channelId() + "-" + proxy.id();
  }
}
