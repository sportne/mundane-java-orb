package io.github.mundanej.mjo.trading;

import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.List;
import java.util.Objects;

final class NetworkTradingServiceState {

  private final LocalOrb orb;
  private final LocalTradingTypeRepository typeRepository;
  private final LocalTradingOfferRepository offerRepository;
  private final LocalObjectReference<NetworkTradingServiceState> traderReference;

  NetworkTradingServiceState(
      LocalOrb orb,
      TradingServiceOptions typeOptions,
      TradingOfferRepositoryOptions offerOptions,
      TradingImportExportOptions importExportOptions) {
    this.orb = Objects.requireNonNull(orb, "orb");
    typeRepository = new LocalTradingTypeRepository(typeOptions);
    offerRepository =
        new LocalTradingOfferRepository(typeRepository, offerOptions, importExportOptions);
    traderReference =
        orb.bindWithObjectId(
            NetworkTradingServiceState.class,
            TradingServiceDescriptors.TRADER,
            NetworkTradingService.DEFAULT_OBJECT_ID,
            new TradingServiceDispatcher(this));
  }

  LocalOrb orb() {
    return orb;
  }

  LocalObjectReference<NetworkTradingServiceState> traderReference() {
    return traderReference;
  }

  TradingServiceType registerType(TradingServiceType type) {
    return typeRepository.register(type);
  }

  TradingServiceType updateType(TradingServiceType type) {
    return typeRepository.update(type);
  }

  TradingServiceType deleteType(String name) {
    return typeRepository.delete(name);
  }

  TradingServiceType lookupType(String name) {
    return typeRepository
        .lookup(name)
        .orElseThrow(
            () ->
                new TradingServiceException(
                    TradingServiceDiagnosticCodes.TYPE_NOT_FOUND, "unknown service type: " + name));
  }

  List<TradingServiceType> listTypes() {
    return typeRepository.list();
  }

  TradingOffer registerOffer(TradingOffer offer) {
    return offerRepository.register(offer);
  }

  TradingOffer withdrawOffer(String offerId) {
    return offerRepository.withdraw(offerId);
  }

  List<TradingOffer> queryOffers(String typeName, String constraintExpression) {
    return offerRepository.query(typeName, constraintExpression);
  }

  TradingImportExportLink registerImportLink(String name, String peerTraderName) {
    return offerRepository.registerImportExportLink(
        new TradingImportExportLink(name, TradingImportExportDirection.IMPORT, peerTraderName));
  }

  TradingImportExportLink registerExportLink(String name, String peerTraderName) {
    return offerRepository.registerImportExportLink(
        new TradingImportExportLink(name, TradingImportExportDirection.EXPORT, peerTraderName));
  }

  List<TradingImportExportLink> listImportExportLinks() {
    return offerRepository.listImportExportLinks();
  }

  void rejectRemoteImportQuery(String linkName) {
    offerRepository.rejectRemoteImportQuery(linkName);
  }
}
