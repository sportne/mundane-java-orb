package io.github.mundanej.mjo.time;

import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopOrbServerHandler;
import io.github.mundanej.mjo.iiop.IiopServer;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.naming.server.NetworkNamingClient;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.Objects;

/** Loopback IIOP server helper for the supported CosTime TimeService subset. */
public final class NetworkTimeService implements AutoCloseable {

  /** Default deterministic object key used by test and interop Time Service lanes. */
  public static final String DEFAULT_OBJECT_ID = "TimeService";

  private final LocalOrb orb;
  private final LocalObjectReference<LocalTimeService> localReference;
  private final IiopServer server;
  private final IiopObjectReference objectReference;

  private NetworkTimeService(
      LocalOrb orb,
      LocalObjectReference<LocalTimeService> localReference,
      IiopServer server,
      IiopObjectReference objectReference) {
    this.orb = Objects.requireNonNull(orb, "orb");
    this.localReference = Objects.requireNonNull(localReference, "localReference");
    this.server = Objects.requireNonNull(server, "server");
    this.objectReference = Objects.requireNonNull(objectReference, "objectReference");
  }

  /** Starts a loopback Time Service with the default object id. */
  public static NetworkTimeService bind(
      IiopEndpoint endpoint, IiopOptions options, LocalTimeService service) {
    return bind(endpoint, options, service, DEFAULT_OBJECT_ID);
  }

  /** Starts a loopback Time Service with an explicit object id. */
  public static NetworkTimeService bind(
      IiopEndpoint endpoint, IiopOptions options, LocalTimeService service, String objectId) {
    Objects.requireNonNull(endpoint, "endpoint");
    Objects.requireNonNull(options, "options");
    Objects.requireNonNull(service, "service");
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<LocalTimeService> localReference =
        orb.bindWithObjectId(
            LocalTimeService.class,
            TimeServiceDescriptors.TIME_SERVICE,
            objectId,
            new TimeServiceDispatcher(service));
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .bind(localReference, TimeServiceDescriptors.iiopOperationBindings())
            .build();
    IiopServer server = IiopServer.bind(endpoint, options, handler);
    IiopObjectReference objectReference =
        IiopObjectReference.fromLocal(server.endpoint(), localReference);
    return new NetworkTimeService(orb, localReference, server, objectReference);
  }

  /** Returns the actual endpoint bound by the IIOP server. */
  public IiopEndpoint endpoint() {
    return server.endpoint();
  }

  /** Returns the local ORB reference owned by this network helper. */
  public LocalObjectReference<LocalTimeService> localReference() {
    return localReference;
  }

  /** Returns the network IIOP object reference for clients. */
  public IiopObjectReference objectReference() {
    return objectReference;
  }

  /** Returns the Time Service IOR. */
  public Ior ior() {
    return objectReference.ior();
  }

  /** Binds this Time Service IOR into a network Naming Service. */
  public void bindInNaming(NetworkNamingClient namingClient, NamingName name) {
    Objects.requireNonNull(namingClient, "namingClient").bind(name, ior());
  }

  /** Binds or replaces this Time Service IOR in a network Naming Service. */
  public void rebindInNaming(NetworkNamingClient namingClient, NamingName name) {
    Objects.requireNonNull(namingClient, "namingClient").rebind(name, ior());
  }

  @Override
  public void close() {
    try {
      server.close();
    } finally {
      orb.shutdown();
    }
  }
}
