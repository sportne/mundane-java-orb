package io.github.mundanej.mjo.naming;

import java.util.List;
import java.util.Optional;

/** Iterator over a stable snapshot of naming bindings. */
public interface NamingBindingIterator {

  /** Returns the next binding, or empty after the snapshot is exhausted. */
  Optional<NamingBinding> nextOne();

  /** Returns up to {@code count} next bindings. */
  List<NamingBinding> next(int count);

  /** Closes this iterator. */
  void destroy();
}
