package io.github.mundanej.mjo.trading;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory import/export boundary metadata for the supported local Trading Service subset. */
public final class LocalTradingImportExportBoundary {

  private final TradingImportExportOptions options;
  private final Map<String, TradingImportExportLink> links = new LinkedHashMap<>();

  /** Creates an import/export boundary with default local Trading Service limits. */
  public LocalTradingImportExportBoundary() {
    this(TradingImportExportOptions.defaults());
  }

  /** Creates an import/export boundary with caller-provided local Trading Service limits. */
  public LocalTradingImportExportBoundary(TradingImportExportOptions options) {
    this.options = java.util.Objects.requireNonNull(options, "options");
  }

  /** Registers a new import/export link. */
  public synchronized TradingImportExportLink register(TradingImportExportLink link) {
    TradingImportExportLink validated = TradingImportExportLink.validateForBoundary(link, options);
    if (links.containsKey(validated.name())) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.LINK_ALREADY_EXISTS,
          "import/export link already exists: " + validated.name());
    }
    if (links.size() >= options.maxLinks()) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.LINK_LIMIT_EXCEEDED,
          "import/export boundary has reached " + options.maxLinks() + " links");
    }
    links.put(validated.name(), validated);
    return validated;
  }

  /** Removes an existing import/export link. */
  public synchronized TradingImportExportLink remove(String linkName) {
    String name = requireLinkName(linkName);
    TradingImportExportLink removed = links.remove(name);
    if (removed == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.LINK_NOT_FOUND, "unknown import/export link: " + name);
    }
    return removed;
  }

  /** Looks up an import/export link by name. */
  public synchronized Optional<TradingImportExportLink> lookup(String linkName) {
    return Optional.ofNullable(links.get(requireLinkName(linkName)));
  }

  /** Lists import/export links in deterministic registration order. */
  public synchronized List<TradingImportExportLink> list() {
    return List.copyOf(links.values());
  }

  /** Lists import/export links for one direction in deterministic registration order. */
  public synchronized List<TradingImportExportLink> list(TradingImportExportDirection direction) {
    TradingImportExportDirection checkedDirection = requireDirection(direction);
    return links.values().stream().filter(link -> link.direction() == checkedDirection).toList();
  }

  /** Rejects remote query traversal for this metadata-only local subset. */
  public synchronized void rejectRemoteQuery(String linkName) {
    TradingImportExportLink link = requireLink(linkName);
    if (link.direction() != TradingImportExportDirection.IMPORT) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.LINK_DIRECTION_MISMATCH,
          "remote query requires an import link: " + link.name());
    }
    throw new TradingServiceException(
        TradingServiceDiagnosticCodes.REMOTE_FEDERATION_DISABLED,
        "remote Trading Service federation is disabled for import link: " + link.name());
  }

  /** Returns the configured boundary limits. */
  public TradingImportExportOptions options() {
    return options;
  }

  private TradingImportExportLink requireLink(String linkName) {
    String name = requireLinkName(linkName);
    TradingImportExportLink link = links.get(name);
    if (link == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.LINK_NOT_FOUND, "unknown import/export link: " + name);
    }
    return link;
  }

  private String requireLinkName(String linkName) {
    return TradingNames.requireBoundedText(
        linkName, "import/export link name", options.maxLinkNameLength());
  }

  private static TradingImportExportDirection requireDirection(
      TradingImportExportDirection direction) {
    if (direction == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_LINK, "link direction must not be null");
    }
    return direction;
  }
}
