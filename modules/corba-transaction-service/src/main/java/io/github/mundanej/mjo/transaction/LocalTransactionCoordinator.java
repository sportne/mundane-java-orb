package io.github.mundanej.mjo.transaction;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** In-memory coordinator/resource model for the supported local Transaction Service subset. */
public final class LocalTransactionCoordinator {

  private final TransactionServiceOptions options;
  private final TransactionTimeoutPolicy timeoutPolicy;
  private final Clock clock;
  private final Map<String, TransactionEntry> transactions = new LinkedHashMap<>();
  private long nextTransactionGeneration = 1;

  /** Creates a coordinator with default local Transaction Service limits. */
  public LocalTransactionCoordinator() {
    this(TransactionServiceOptions.defaults());
  }

  /** Creates a coordinator with caller-provided local Transaction Service limits. */
  public LocalTransactionCoordinator(TransactionServiceOptions options) {
    this(options, TransactionTimeoutPolicy.defaults(), Clock.systemUTC());
  }

  /** Creates a coordinator with caller-provided limits, timeout policy, and clock. */
  public LocalTransactionCoordinator(
      TransactionServiceOptions options, TransactionTimeoutPolicy timeoutPolicy, Clock clock) {
    this.options = Objects.requireNonNull(options, "options");
    this.timeoutPolicy = Objects.requireNonNull(timeoutPolicy, "timeoutPolicy");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Creates a new local transaction entry. */
  public synchronized TransactionHandle begin(String transactionId) {
    return begin(transactionId, null);
  }

  /** Creates a new local transaction entry with a caller-requested timeout. */
  public synchronized TransactionHandle begin(String transactionId, Duration timeout) {
    String id = TransactionNames.requireIdentifier(transactionId, "transaction ID", options);
    if (transactions.containsKey(id)) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.TRANSACTION_ALREADY_EXISTS,
          "transaction already exists: " + id);
    }
    if (transactions.size() >= options.maxTransactions()) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.TRANSACTION_LIMIT_EXCEEDED,
          "transaction coordinator has reached " + options.maxTransactions() + " transactions");
    }
    Instant beganAt = clock.instant();
    TransactionEntry entry =
        new TransactionEntry(
            new TransactionId(id),
            nextTransactionGeneration++,
            beganAt,
            timeoutPolicy.deadlineFor(beganAt, timeout));
    transactions.put(id, entry);
    return entry.handle();
  }

  /** Looks up a local transaction by ID. */
  public synchronized Optional<TransactionSnapshot> lookup(String transactionId) {
    String id = TransactionNames.requireIdentifier(transactionId, "transaction ID", options);
    TransactionEntry entry = transactions.get(id);
    return entry == null ? Optional.empty() : Optional.of(entry.snapshot());
  }

  /** Returns a snapshot through a current local transaction handle. */
  public synchronized TransactionSnapshot snapshot(TransactionHandle handle) {
    return requireCurrentTransaction(handle).snapshot();
  }

  /** Removes a local transaction entry without adding completion semantics. */
  public synchronized TransactionSnapshot forget(String transactionId) {
    String id = TransactionNames.requireIdentifier(transactionId, "transaction ID", options);
    TransactionEntry removed = transactions.remove(id);
    if (removed == null) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.TRANSACTION_NOT_FOUND, "unknown transaction: " + id);
    }
    return removed.snapshot();
  }

  /** Enlists a resource with a local transaction. */
  public synchronized TransactionResourceHandle enlist(
      TransactionHandle handle, String resourceId) {
    TransactionEntry entry = requireCurrentTransaction(handle);
    String id = TransactionNames.requireIdentifier(resourceId, "resource ID", options);
    if (entry.resources.containsKey(id)) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.RESOURCE_ALREADY_ENLISTED,
          "resource already enlisted: " + id);
    }
    if (entry.resources.size() >= options.maxResourcesPerTransaction()) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.RESOURCE_LIMIT_EXCEEDED,
          "transaction has reached "
              + options.maxResourcesPerTransaction()
              + " enlisted resources");
    }
    ResourceEntry resource =
        new ResourceEntry(new TransactionResourceId(id), entry.nextResourceGeneration++);
    entry.resources.put(id, resource);
    return resource.handle(entry);
  }

  /** Delists a resource by transaction ID and resource ID. */
  public synchronized TransactionResourceSnapshot delist(String transactionId, String resourceId) {
    String txId = TransactionNames.requireIdentifier(transactionId, "transaction ID", options);
    String resId = TransactionNames.requireIdentifier(resourceId, "resource ID", options);
    TransactionEntry entry = transactions.get(txId);
    if (entry == null) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.TRANSACTION_NOT_FOUND, "unknown transaction: " + txId);
    }
    if (entry.isExpired(clock.instant())) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.TRANSACTION_EXPIRED, "transaction expired: " + txId);
    }
    ResourceEntry removed = entry.resources.remove(resId);
    if (removed == null) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.RESOURCE_NOT_FOUND, "unknown resource: " + resId);
    }
    return removed.snapshot();
  }

  /** Delists a resource through a current local resource handle. */
  public synchronized TransactionResourceSnapshot delist(TransactionResourceHandle handle) {
    Objects.requireNonNull(handle, "handle");
    TransactionEntry entry =
        requireCurrentTransaction(
            new TransactionHandle(handle.transactionId(), handle.transactionGeneration()));
    ResourceEntry resource = entry.resources.get(handle.resourceId().value());
    if (resource == null || resource.generation != handle.resourceGeneration()) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.STALE_RESOURCE,
          "stale resource handle: " + handle.resourceId().value());
    }
    entry.resources.remove(handle.resourceId().value());
    return resource.snapshot();
  }

  /** Lists local transactions in deterministic coordinator insertion order. */
  public synchronized List<TransactionSnapshot> list() {
    return transactions.values().stream().map(TransactionEntry::snapshot).toList();
  }

  /** Returns the configured coordinator limits. */
  public TransactionServiceOptions options() {
    return options;
  }

  private TransactionEntry requireCurrentTransaction(TransactionHandle handle) {
    Objects.requireNonNull(handle, "handle");
    TransactionEntry entry = transactions.get(handle.transactionId().value());
    if (entry == null) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.TRANSACTION_NOT_FOUND,
          "unknown transaction: " + handle.transactionId().value());
    }
    if (entry.generation != handle.generation()) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.STALE_TRANSACTION,
          "stale transaction handle: " + handle.transactionId().value());
    }
    if (entry.isExpired(clock.instant())) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.TRANSACTION_EXPIRED,
          "transaction expired: " + handle.transactionId().value());
    }
    return entry;
  }

  private static final class TransactionEntry {
    private final TransactionId id;
    private final long generation;
    private final Instant beganAt;
    private final Instant expiresAt;
    private final Map<String, ResourceEntry> resources = new LinkedHashMap<>();
    private long nextResourceGeneration = 1;

    private TransactionEntry(
        TransactionId id, long generation, Instant beganAt, Instant expiresAt) {
      this.id = id;
      this.generation = generation;
      this.beganAt = beganAt;
      this.expiresAt = expiresAt;
    }

    private TransactionHandle handle() {
      return new TransactionHandle(id, generation);
    }

    private TransactionSnapshot snapshot() {
      return new TransactionSnapshot(
          id,
          beganAt,
          expiresAt,
          resources.values().stream().map(ResourceEntry::snapshot).toList());
    }

    private boolean isExpired(Instant now) {
      return !now.isBefore(expiresAt);
    }
  }

  private static final class ResourceEntry {
    private final TransactionResourceId id;
    private final long generation;

    private ResourceEntry(TransactionResourceId id, long generation) {
      this.id = id;
      this.generation = generation;
    }

    private TransactionResourceHandle handle(TransactionEntry transaction) {
      return new TransactionResourceHandle(transaction.id, transaction.generation, id, generation);
    }

    private TransactionResourceSnapshot snapshot() {
      return new TransactionResourceSnapshot(id);
    }
  }
}
