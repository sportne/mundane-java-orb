package io.github.mundanej.mjo.naming;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a NamingContext list operation.
 *
 * @param bindings immediate bindings returned inline
 * @param iterator remaining binding iterator when more entries exist
 */
public record NamingListResult(
    List<NamingBinding> bindings, Optional<NamingBindingIterator> iterator) {

  /** Creates an immutable list result. */
  public NamingListResult {
    bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings"));
    Objects.requireNonNull(iterator, "iterator");
  }
}
