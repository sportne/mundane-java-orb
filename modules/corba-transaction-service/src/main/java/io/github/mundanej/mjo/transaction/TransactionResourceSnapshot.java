package io.github.mundanej.mjo.transaction;

import java.util.Objects;

/** Immutable snapshot of one resource enlistment in a local transaction. */
public record TransactionResourceSnapshot(TransactionResourceId resourceId) {

  /** Creates a resource snapshot. */
  public TransactionResourceSnapshot {
    Objects.requireNonNull(resourceId, "resourceId");
  }
}
