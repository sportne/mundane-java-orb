package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopOrbServerHandler;
import io.github.mundanej.mjo.iiop.IiopServer;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.naming.server.NetworkNamingClient;
import java.util.Objects;

/** Loopback IIOP server helper for the supported Event Service subset. */
public final class NetworkEventService implements AutoCloseable {

  /** Default deterministic object key used by Event Service tests and interop lanes. */
  public static final String DEFAULT_OBJECT_ID = "EventChannel";

  private final LocalEventService service;
  private final NetworkEventServiceState state;
  private final IiopServer server;
  private final IiopObjectReference objectReference;

  private NetworkEventService(
      LocalEventService service,
      NetworkEventServiceState state,
      IiopServer server,
      IiopObjectReference objectReference) {
    this.service = Objects.requireNonNull(service, "service");
    this.state = Objects.requireNonNull(state, "state");
    this.server = Objects.requireNonNull(server, "server");
    this.objectReference = Objects.requireNonNull(objectReference, "objectReference");
  }

  /** Starts a loopback Event Service with default local options. */
  public static NetworkEventService bind(IiopEndpoint endpoint, IiopOptions options) {
    return bind(endpoint, options, EventServiceOptions.defaults());
  }

  /** Starts a loopback Event Service with explicit local options. */
  public static NetworkEventService bind(
      IiopEndpoint endpoint, IiopOptions options, EventServiceOptions eventOptions) {
    Objects.requireNonNull(endpoint, "endpoint");
    Objects.requireNonNull(options, "options");
    LocalEventService service = LocalEventService.create(eventOptions);
    NetworkEventServiceState state =
        new NetworkEventServiceState(
            io.github.mundanej.mjo.orb.LocalOrb.create(), service.createChannel());
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(state.orb())
            .bind(state.channelReference(), EventServiceDescriptors.eventChannelBindings())
            .bind(state.supplierAdminReference(), EventServiceDescriptors.supplierAdminBindings())
            .bind(state.consumerAdminReference(), EventServiceDescriptors.consumerAdminBindings())
            .bindDescriptor(
                EventServiceDescriptors.PROXY_PUSH_CONSUMER,
                EventServiceDescriptors.proxyPushConsumerBindings())
            .bindDescriptor(
                EventServiceDescriptors.PROXY_PULL_CONSUMER,
                EventServiceDescriptors.proxyPullConsumerBindings())
            .bindDescriptor(
                EventServiceDescriptors.PROXY_PUSH_SUPPLIER,
                EventServiceDescriptors.proxyPushSupplierBindings())
            .bindDescriptor(
                EventServiceDescriptors.PROXY_PULL_SUPPLIER,
                EventServiceDescriptors.proxyPullSupplierBindings())
            .durableObjectResolver(state::resolve)
            .build();
    IiopServer server = IiopServer.bind(endpoint, options, handler);
    try {
      state.endpoint(server.endpoint());
      IiopObjectReference objectReference =
          IiopObjectReference.fromLocal(server.endpoint(), state.channelReference());
      return new NetworkEventService(service, state, server, objectReference);
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

  /** Returns the local Event Service owned by this network helper. */
  public LocalEventService service() {
    return service;
  }

  /** Returns the default local EventChannel exposed by this network helper. */
  public LocalEventChannel channel() {
    return state.channel();
  }

  /** Returns the network IIOP object reference for the default EventChannel. */
  public IiopObjectReference objectReference() {
    return objectReference;
  }

  /** Returns the default EventChannel IOR. */
  public Ior ior() {
    return objectReference.ior();
  }

  /** Binds this EventChannel IOR into a network Naming Service. */
  public void bindInNaming(NetworkNamingClient namingClient, NamingName name) {
    Objects.requireNonNull(namingClient, "namingClient").bind(name, ior());
  }

  /** Binds or replaces this EventChannel IOR in a network Naming Service. */
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
