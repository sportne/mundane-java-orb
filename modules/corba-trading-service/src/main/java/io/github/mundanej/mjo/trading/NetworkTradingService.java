package io.github.mundanej.mjo.trading;

import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopOrbServerHandler;
import io.github.mundanej.mjo.iiop.IiopServer;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.naming.server.NetworkNamingClient;
import java.util.Objects;

/** Loopback IIOP server helper for the supported Trading Service subset. */
public final class NetworkTradingService implements AutoCloseable {

  /** Default deterministic object key used by Trading Service tests and interop lanes. */
  public static final String DEFAULT_OBJECT_ID = "Trader";

  private final NetworkTradingServiceState state;
  private final IiopServer server;
  private final IiopObjectReference objectReference;

  private NetworkTradingService(
      NetworkTradingServiceState state, IiopServer server, IiopObjectReference objectReference) {
    this.state = Objects.requireNonNull(state, "state");
    this.server = Objects.requireNonNull(server, "server");
    this.objectReference = Objects.requireNonNull(objectReference, "objectReference");
  }

  /** Starts a loopback Trading Service with default local options. */
  public static NetworkTradingService bind(IiopEndpoint endpoint, IiopOptions options) {
    return bind(
        endpoint,
        options,
        TradingServiceOptions.defaults(),
        TradingOfferRepositoryOptions.defaults(),
        TradingImportExportOptions.defaults());
  }

  /** Starts a loopback Trading Service with explicit local options. */
  public static NetworkTradingService bind(
      IiopEndpoint endpoint,
      IiopOptions options,
      TradingServiceOptions typeOptions,
      TradingOfferRepositoryOptions offerOptions,
      TradingImportExportOptions importExportOptions) {
    Objects.requireNonNull(endpoint, "endpoint");
    Objects.requireNonNull(options, "options");
    NetworkTradingServiceState state =
        new NetworkTradingServiceState(
            io.github.mundanej.mjo.orb.LocalOrb.create(),
            typeOptions,
            offerOptions,
            importExportOptions);
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(state.orb())
            .bind(state.traderReference(), TradingServiceDescriptors.iiopOperationBindings())
            .build();
    IiopServer server = IiopServer.bind(endpoint, options, handler);
    try {
      IiopObjectReference objectReference =
          IiopObjectReference.fromLocal(server.endpoint(), state.traderReference());
      return new NetworkTradingService(state, server, objectReference);
    } catch (RuntimeException exception) {
      server.close();
      state.orb().shutdown();
      throw exception;
    }
  }

  /** Returns the actual endpoint bound by the IIOP server. */
  public IiopEndpoint endpoint() {
    return server.endpoint();
  }

  /** Returns the network IIOP object reference for the supported Trader facade. */
  public IiopObjectReference objectReference() {
    return objectReference;
  }

  /** Returns the supported Trader IOR. */
  public Ior ior() {
    return objectReference.ior();
  }

  /** Binds this Trader IOR into a network Naming Service. */
  public void bindInNaming(NetworkNamingClient namingClient, NamingName name) {
    Objects.requireNonNull(namingClient, "namingClient").bind(name, ior());
  }

  /** Binds or replaces this Trader IOR in a network Naming Service. */
  public void rebindInNaming(NetworkNamingClient namingClient, NamingName name) {
    Objects.requireNonNull(namingClient, "namingClient").rebind(name, ior());
  }

  @Override
  public void close() {
    try {
      server.close();
    } finally {
      state.orb().shutdown();
    }
  }
}
