package io.github.mundanej.mjo.notification;

import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopOrbServerHandler;
import io.github.mundanej.mjo.iiop.IiopServer;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.naming.server.NetworkNamingClient;
import java.util.Objects;

/** Loopback IIOP server helper for the supported Notification Service subset. */
public final class NetworkNotificationService implements AutoCloseable {

  /** Default deterministic object key used by Notification Service tests. */
  public static final String DEFAULT_OBJECT_ID = "NotificationChannel";

  private final LocalNotificationService service;
  private final NetworkNotificationServiceState state;
  private final IiopServer server;
  private final IiopObjectReference objectReference;

  private NetworkNotificationService(
      LocalNotificationService service,
      NetworkNotificationServiceState state,
      IiopServer server,
      IiopObjectReference objectReference) {
    this.service = Objects.requireNonNull(service, "service");
    this.state = Objects.requireNonNull(state, "state");
    this.server = Objects.requireNonNull(server, "server");
    this.objectReference = Objects.requireNonNull(objectReference, "objectReference");
  }

  /** Starts a loopback Notification Service with default local options. */
  public static NetworkNotificationService bind(IiopEndpoint endpoint, IiopOptions options) {
    return bind(endpoint, options, NotificationServiceOptions.defaults());
  }

  /** Starts a loopback Notification Service with explicit local options. */
  public static NetworkNotificationService bind(
      IiopEndpoint endpoint, IiopOptions options, NotificationServiceOptions notificationOptions) {
    Objects.requireNonNull(endpoint, "endpoint");
    Objects.requireNonNull(options, "options");
    LocalNotificationService service = LocalNotificationService.create(notificationOptions);
    NetworkNotificationServiceState state =
        new NetworkNotificationServiceState(
            io.github.mundanej.mjo.orb.LocalOrb.create(), service.createChannel());
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(state.orb())
            .bind(state.channelReference(), NotificationServiceDescriptors.eventChannelBindings())
            .bind(
                state.supplierAdminReference(),
                NotificationServiceDescriptors.supplierAdminBindings())
            .bind(
                state.consumerAdminReference(),
                NotificationServiceDescriptors.consumerAdminBindings())
            .bindDescriptor(
                NotificationServiceDescriptors.STRUCTURED_PUSH_CONSUMER,
                NotificationServiceDescriptors.structuredPushConsumerBindings())
            .bindDescriptor(
                NotificationServiceDescriptors.STRUCTURED_PULL_CONSUMER,
                NotificationServiceDescriptors.structuredPullConsumerBindings())
            .bindDescriptor(
                NotificationServiceDescriptors.STRUCTURED_PUSH_SUPPLIER,
                NotificationServiceDescriptors.structuredPushSupplierBindings())
            .bindDescriptor(
                NotificationServiceDescriptors.STRUCTURED_PULL_SUPPLIER,
                NotificationServiceDescriptors.structuredPullSupplierBindings())
            .durableObjectResolver(state::resolve)
            .build();
    IiopServer server = IiopServer.bind(endpoint, options, handler);
    try {
      state.endpoint(server.endpoint());
      IiopObjectReference objectReference =
          IiopObjectReference.fromLocal(server.endpoint(), state.channelReference());
      return new NetworkNotificationService(service, state, server, objectReference);
    } catch (RuntimeException exception) {
      server.close();
      state.orb().shutdown();
      service.close();
      throw exception;
    }
  }

  /** Returns the actual endpoint bound by the IIOP server. */
  public IiopEndpoint endpoint() {
    return server.endpoint();
  }

  /** Returns the local Notification Service owned by this network helper. */
  public LocalNotificationService service() {
    return service;
  }

  /** Returns the default local Notification EventChannel exposed by this helper. */
  public LocalNotificationChannel channel() {
    return state.channel();
  }

  /** Returns the network IIOP object reference for the default Notification EventChannel. */
  public IiopObjectReference objectReference() {
    return objectReference;
  }

  /** Returns the default Notification EventChannel IOR. */
  public Ior ior() {
    return objectReference.ior();
  }

  /** Binds this Notification EventChannel IOR into a network Naming Service. */
  public void bindInNaming(NetworkNamingClient namingClient, NamingName name) {
    Objects.requireNonNull(namingClient, "namingClient").bind(name, ior());
  }

  /** Binds or replaces this Notification EventChannel IOR in a network Naming Service. */
  public void rebindInNaming(NetworkNamingClient namingClient, NamingName name) {
    Objects.requireNonNull(namingClient, "namingClient").rebind(name, ior());
  }

  @Override
  public void close() {
    try {
      server.close();
    } finally {
      try {
        state.orb().shutdown();
      } finally {
        service.close();
      }
    }
  }
}
