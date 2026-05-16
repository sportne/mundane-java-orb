package io.github.mundanej.mjo.repositoryid;

/** Thrown when a repository ID string or component does not satisfy the supported value rules. */
public final class RepositoryIdException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  /** Creates a repository ID validation exception. */
  public RepositoryIdException(String message) {
    super(message);
  }
}
