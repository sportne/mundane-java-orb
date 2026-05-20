package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * Repository ID values planned for one Java-to-IDL model.
 *
 * @param repositoryIds planned repository IDs in deterministic model traversal order
 */
public record RmiRepositoryIdPlan(List<RmiRepositoryIdValue> repositoryIds) {

  /** Creates an immutable repository ID plan. */
  public RmiRepositoryIdPlan {
    repositoryIds = List.copyOf(Objects.requireNonNull(repositoryIds, "repositoryIds"));
  }
}
