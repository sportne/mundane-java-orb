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
    if (entry == null) {
      return Optional.empty();
    }
    applyTimeoutRollback(entry);
    return Optional.of(entry.snapshot());
  }

  /** Returns a snapshot through a current local transaction handle. */
  public synchronized TransactionSnapshot snapshot(TransactionHandle handle) {
    TransactionEntry entry = requireCurrentTransaction(handle);
    if (entry.isActive() && entry.isExpired(clock.instant())) {
      rollbackExpired(entry);
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.TRANSACTION_EXPIRED,
          "transaction expired: " + handle.transactionId().value());
    }
    return entry.snapshot();
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
    return enlist(handle, resourceId, TransactionResourceParticipant.noOp());
  }

  /** Enlists a resource participant with a local transaction. */
  public synchronized TransactionResourceHandle enlist(
      TransactionHandle handle, String resourceId, TransactionResourceParticipant participant) {
    TransactionEntry entry = requireActiveTransaction(handle);
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
        new ResourceEntry(
            new TransactionResourceId(id),
            entry.nextResourceGeneration++,
            Objects.requireNonNull(participant, "participant"));
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
    requireActiveState(entry);
    requireNotExpired(entry);
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
        requireActiveTransaction(
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

  /** Marks an active local transaction for rollback. */
  public synchronized TransactionSnapshot markRollbackOnly(TransactionHandle handle) {
    TransactionEntry entry = requireActiveTransaction(handle);
    entry.state = TransactionState.ROLLBACK_ONLY;
    return entry.snapshot();
  }

  /** Completes an active local transaction through deterministic local commit callbacks. */
  public synchronized TransactionSnapshot commit(TransactionHandle handle) {
    TransactionEntry entry = requireCurrentTransaction(handle);
    requireActiveState(entry);
    if (entry.isExpired(clock.instant())) {
      rollbackExpired(entry);
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.TRANSACTION_EXPIRED,
          "transaction expired: " + handle.transactionId().value());
    }
    if (entry.state == TransactionState.ROLLBACK_ONLY) {
      rollbackResources(entry, TransactionState.ROLLED_BACK);
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.ROLLBACK_ONLY,
          "transaction marked rollback-only: " + handle.transactionId().value());
    }
    prepareResources(entry);
    commitResources(entry);
    entry.state = TransactionState.COMMITTED;
    entry.resources.clear();
    return entry.snapshot();
  }

  /** Completes an active local transaction through deterministic local rollback callbacks. */
  public synchronized TransactionSnapshot rollback(TransactionHandle handle) {
    TransactionEntry entry = requireCurrentTransaction(handle);
    requireActiveState(entry);
    if (entry.isExpired(clock.instant())) {
      rollbackExpired(entry);
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.TRANSACTION_EXPIRED,
          "transaction expired: " + handle.transactionId().value());
    }
    rollbackResources(entry, TransactionState.ROLLED_BACK);
    return entry.snapshot();
  }

  /** Lists local transactions in deterministic coordinator insertion order. */
  public synchronized List<TransactionSnapshot> list() {
    transactions.values().forEach(this::applyTimeoutRollback);
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
    return entry;
  }

  private TransactionEntry requireActiveTransaction(TransactionHandle handle) {
    TransactionEntry entry = requireCurrentTransaction(handle);
    requireActiveState(entry);
    requireNotExpired(entry);
    return entry;
  }

  private void requireActiveState(TransactionEntry entry) {
    if (!entry.isActive()) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.ILLEGAL_TRANSACTION_STATE,
          "transaction is no longer active: " + entry.id.value());
    }
  }

  private void requireNotExpired(TransactionEntry entry) {
    if (entry.isExpired(clock.instant())) {
      rollbackExpired(entry);
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.TRANSACTION_EXPIRED,
          "transaction expired: " + entry.id.value());
    }
  }

  private void prepareResources(TransactionEntry entry) {
    List<ResourceEntry> resources = List.copyOf(entry.resources.values());
    for (ResourceEntry resource : resources) {
      TransactionResourceVote vote;
      try {
        vote = Objects.requireNonNull(resource.participant.prepare(), "prepare vote");
      } catch (RuntimeException exception) {
        rollbackResources(entry, TransactionState.ROLLED_BACK);
        throw new TransactionServiceException(
            TransactionServiceDiagnosticCodes.RESOURCE_PREPARE_FAILED,
            "resource prepare failed: " + resource.id.value());
      }
      if (vote == TransactionResourceVote.ROLLBACK) {
        rollbackResources(entry, TransactionState.ROLLED_BACK);
        throw new TransactionServiceException(
            TransactionServiceDiagnosticCodes.RESOURCE_PREPARE_FAILED,
            "resource voted rollback: " + resource.id.value());
      }
    }
  }

  private void commitResources(TransactionEntry entry) {
    List<ResourceEntry> resources = List.copyOf(entry.resources.values());
    for (ResourceEntry resource : resources) {
      try {
        resource.participant.commit();
      } catch (RuntimeException exception) {
        entry.state = TransactionState.HEURISTIC_MIXED;
        entry.resources.clear();
        throw new TransactionServiceException(
            TransactionServiceDiagnosticCodes.RESOURCE_COMMIT_FAILED,
            "resource commit failed: " + resource.id.value());
      }
    }
  }

  private void rollbackExpired(TransactionEntry entry) {
    if (entry.isActive()) {
      rollbackResources(entry, TransactionState.TIMEOUT_ROLLED_BACK);
    }
  }

  private void applyTimeoutRollback(TransactionEntry entry) {
    if (entry.isActive() && entry.isExpired(clock.instant())) {
      rollbackExpired(entry);
    }
  }

  private void rollbackResources(TransactionEntry entry, TransactionState terminalState) {
    List<ResourceEntry> resources = List.copyOf(entry.resources.values());
    for (ResourceEntry resource : resources) {
      try {
        resource.participant.rollback();
      } catch (RuntimeException exception) {
        entry.state = TransactionState.HEURISTIC_MIXED;
        entry.resources.clear();
        throw new TransactionServiceException(
            TransactionServiceDiagnosticCodes.RESOURCE_ROLLBACK_FAILED,
            "resource rollback failed: " + resource.id.value());
      }
    }
    entry.state = terminalState;
    entry.resources.clear();
  }

  private static final class TransactionEntry {
    private final TransactionId id;
    private final long generation;
    private final Instant beganAt;
    private final Instant expiresAt;
    private final Map<String, ResourceEntry> resources = new LinkedHashMap<>();
    private TransactionState state = TransactionState.ACTIVE;
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
          state,
          beganAt,
          expiresAt,
          resources.values().stream().map(ResourceEntry::snapshot).toList());
    }

    private boolean isActive() {
      return state == TransactionState.ACTIVE || state == TransactionState.ROLLBACK_ONLY;
    }

    private boolean isExpired(Instant now) {
      return !now.isBefore(expiresAt);
    }
  }

  private static final class ResourceEntry {
    private final TransactionResourceId id;
    private final long generation;
    private final TransactionResourceParticipant participant;

    private ResourceEntry(
        TransactionResourceId id, long generation, TransactionResourceParticipant participant) {
      this.id = id;
      this.generation = generation;
      this.participant = participant;
    }

    private TransactionResourceHandle handle(TransactionEntry transaction) {
      return new TransactionResourceHandle(transaction.id, transaction.generation, id, generation);
    }

    private TransactionResourceSnapshot snapshot() {
      return new TransactionResourceSnapshot(id);
    }
  }
}
