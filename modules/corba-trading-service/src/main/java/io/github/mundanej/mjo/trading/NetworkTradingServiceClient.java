package io.github.mundanej.mjo.trading;

import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopOrbClient;
import io.github.mundanej.mjo.ior.Ior;
import java.util.List;
import java.util.Objects;

/** Client helper for invoking the supported Trading Service IIOP operations. */
public final class NetworkTradingServiceClient implements AutoCloseable {

  private final IiopOrbClient client;

  private NetworkTradingServiceClient(IiopOrbClient client) {
    this.client = Objects.requireNonNull(client, "client");
  }

  /** Connects to a Trading Service IOR. */
  public static NetworkTradingServiceClient connect(Ior ior, IiopOptions options) {
    return connect(IiopObjectReference.fromIor(ior), options);
  }

  /** Connects to a Trading Service IIOP object reference. */
  public static NetworkTradingServiceClient connect(
      IiopObjectReference reference, IiopOptions options) {
    return new NetworkTradingServiceClient(IiopOrbClient.connect(reference, options));
  }

  /** Invokes Trader::register_type. */
  public void registerType(TradingServiceType type) {
    invoke(TradingServiceDescriptors.REGISTER_TYPE, List.of(type));
  }

  /** Invokes Trader::update_type. */
  public void updateType(TradingServiceType type) {
    invoke(TradingServiceDescriptors.UPDATE_TYPE, List.of(type));
  }

  /** Invokes Trader::delete_type. */
  public TradingServiceType deleteType(String name) {
    return (TradingServiceType) invoke(TradingServiceDescriptors.DELETE_TYPE, List.of(name));
  }

  /** Invokes Trader::lookup_type. */
  public TradingServiceType lookupType(String name) {
    return (TradingServiceType) invoke(TradingServiceDescriptors.LOOKUP_TYPE, List.of(name));
  }

  /** Invokes Trader::list_types. */
  @SuppressWarnings("unchecked")
  public List<TradingServiceType> listTypes() {
    return (List<TradingServiceType>) invoke(TradingServiceDescriptors.LIST_TYPES, List.of());
  }

  /** Invokes Trader::register_offer. */
  public void registerOffer(TradingOffer offer) {
    invoke(TradingServiceDescriptors.REGISTER_OFFER, List.of(offer));
  }

  /** Invokes Trader::withdraw_offer. */
  public TradingOffer withdrawOffer(String offerId) {
    return (TradingOffer) invoke(TradingServiceDescriptors.WITHDRAW_OFFER, List.of(offerId));
  }

  /** Invokes Trader::query_offers. */
  @SuppressWarnings("unchecked")
  public List<TradingOffer> queryOffers(String typeName, String constraintExpression) {
    return (List<TradingOffer>)
        invoke(TradingServiceDescriptors.QUERY_OFFERS, List.of(typeName, constraintExpression));
  }

  /** Invokes Trader::register_import_link. */
  public void registerImportLink(String name, String peerTraderName) {
    invoke(TradingServiceDescriptors.REGISTER_IMPORT_LINK, List.of(name, peerTraderName));
  }

  /** Invokes Trader::register_export_link. */
  public void registerExportLink(String name, String peerTraderName) {
    invoke(TradingServiceDescriptors.REGISTER_EXPORT_LINK, List.of(name, peerTraderName));
  }

  /** Invokes Trader::list_import_export_links. */
  @SuppressWarnings("unchecked")
  public List<TradingImportExportLink> listImportExportLinks() {
    return (List<TradingImportExportLink>)
        invoke(TradingServiceDescriptors.LIST_IMPORT_EXPORT_LINKS, List.of());
  }

  /** Invokes Trader::reject_remote_import_query. */
  public void rejectRemoteImportQuery(String linkName) {
    invoke(TradingServiceDescriptors.REJECT_REMOTE_IMPORT_QUERY, List.of(linkName));
  }

  @Override
  public void close() {
    client.close();
  }

  private Object invoke(
      io.github.mundanej.mjo.typecode.IdlOperationDescriptor operation, List<Object> arguments) {
    return client.invoke(operation, TradingServiceIiopCodec.INSTANCE, arguments);
  }
}
