package io.github.mundanej.mjo.trading;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory offer repository for the supported local Trading Service subset. */
public final class LocalTradingOfferRepository {

  private final LocalTradingTypeRepository typeRepository;
  private final TradingOfferRepositoryOptions options;
  private final Map<String, TradingOffer> offers = new LinkedHashMap<>();

  /** Creates an offer repository with default local Trading Service limits. */
  public LocalTradingOfferRepository(LocalTradingTypeRepository typeRepository) {
    this(typeRepository, TradingOfferRepositoryOptions.defaults());
  }

  /** Creates an offer repository with caller-provided local Trading Service limits. */
  public LocalTradingOfferRepository(
      LocalTradingTypeRepository typeRepository, TradingOfferRepositoryOptions options) {
    this.typeRepository = java.util.Objects.requireNonNull(typeRepository, "typeRepository");
    this.options = java.util.Objects.requireNonNull(options, "options");
  }

  /** Registers a new offer. */
  public synchronized TradingOffer register(TradingOffer offer) {
    TradingOffer validated = validatedOffer(offer);
    if (offers.containsKey(validated.id())) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.OFFER_ALREADY_EXISTS,
          "offer already exists: " + validated.id());
    }
    if (offers.size() >= options.maxOffers()) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.OFFER_LIMIT_EXCEEDED,
          "offer repository has reached " + options.maxOffers() + " offers");
    }
    offers.put(validated.id(), validated);
    return validated;
  }

  /** Replaces an existing offer. */
  public synchronized TradingOffer update(TradingOffer offer) {
    TradingOffer validated = validatedOffer(offer);
    requireExisting(validated.id());
    offers.put(validated.id(), validated);
    return validated;
  }

  /** Withdraws an existing offer. */
  public synchronized TradingOffer withdraw(String offerId) {
    String id = TradingNames.requireBoundedText(offerId, "offer ID", options.maxOfferIdLength());
    TradingOffer removed = offers.remove(id);
    if (removed == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.OFFER_NOT_FOUND, "unknown offer: " + id);
    }
    return removed;
  }

  /** Looks up an offer by ID. */
  public synchronized Optional<TradingOffer> lookup(String offerId) {
    String id = TradingNames.requireBoundedText(offerId, "offer ID", options.maxOfferIdLength());
    return Optional.ofNullable(offers.get(id));
  }

  /** Lists offers for a service type in deterministic repository insertion order. */
  public synchronized List<TradingOffer> listByType(String typeName) {
    TradingServiceType type = requireType(typeName);
    return offers.values().stream().filter(offer -> type.name().equals(offer.typeName())).toList();
  }

  /** Returns the configured repository limits. */
  public TradingOfferRepositoryOptions options() {
    return options;
  }

  private TradingOffer validatedOffer(TradingOffer offer) {
    if (offer == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_OFFER, "offer must not be null");
    }
    TradingServiceType type = requireType(offer.typeName());
    return TradingOffer.validateForRepository(offer, type, typeRepository.options(), options);
  }

  private TradingServiceType requireType(String typeName) {
    String name =
        TradingNames.requireName(typeName, "offer service type name", typeRepository.options());
    return typeRepository
        .lookup(name)
        .orElseThrow(
            () ->
                new TradingServiceException(
                    TradingServiceDiagnosticCodes.TYPE_NOT_FOUND,
                    "unknown service type for offer: " + name));
  }

  private void requireExisting(String offerId) {
    if (!offers.containsKey(offerId)) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.OFFER_NOT_FOUND, "unknown offer: " + offerId);
    }
  }
}
