package io.github.mundanej.mjo.trading;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory service type repository for the supported local Trading Service subset. */
public final class LocalTradingTypeRepository {

  private final TradingServiceOptions options;
  private final Map<String, TradingServiceType> types = new LinkedHashMap<>();

  /** Creates a repository with default local Trading Service limits. */
  public LocalTradingTypeRepository() {
    this(TradingServiceOptions.defaults());
  }

  /** Creates a repository with caller-provided local Trading Service limits. */
  public LocalTradingTypeRepository(TradingServiceOptions options) {
    this.options = java.util.Objects.requireNonNull(options, "options");
  }

  /** Registers a new service type. */
  public synchronized TradingServiceType register(TradingServiceType type) {
    TradingServiceType validated = TradingServiceType.validateForRepository(type, options);
    if (types.containsKey(validated.name())) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.TYPE_ALREADY_EXISTS,
          "service type already exists: " + validated.name());
    }
    if (types.size() >= options.maxTypes()) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.TYPE_LIMIT_EXCEEDED,
          "service type repository has reached " + options.maxTypes() + " types");
    }
    types.put(validated.name(), validated);
    return validated;
  }

  /** Replaces an existing service type definition. */
  public synchronized TradingServiceType update(TradingServiceType type) {
    TradingServiceType validated = TradingServiceType.validateForRepository(type, options);
    requireExisting(validated.name());
    types.put(validated.name(), validated);
    return validated;
  }

  /** Deletes an existing service type definition. */
  public synchronized TradingServiceType delete(String name) {
    String typeName = TradingNames.requireName(name, "service type name", options);
    TradingServiceType removed = types.remove(typeName);
    if (removed == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.TYPE_NOT_FOUND, "unknown service type: " + typeName);
    }
    return removed;
  }

  /** Looks up a service type by name. */
  public synchronized Optional<TradingServiceType> lookup(String name) {
    String typeName = TradingNames.requireName(name, "service type name", options);
    return Optional.ofNullable(types.get(typeName));
  }

  /** Lists service types in deterministic repository insertion order. */
  public synchronized List<TradingServiceType> list() {
    return List.copyOf(types.values());
  }

  /** Returns the configured repository limits. */
  public TradingServiceOptions options() {
    return options;
  }

  private void requireExisting(String typeName) {
    if (!types.containsKey(typeName)) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.TYPE_NOT_FOUND, "unknown service type: " + typeName);
    }
  }
}
