package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/** Declared user exception received from an RMI-IIOP wire reply by repository ID. */
public final class RmiIiopWireUserException extends Exception {

  private static final long serialVersionUID = 1L;

  private final String repositoryId;
  private final transient List<RmiCdrValue> fields;

  /** Creates a wire user-exception marker for a declared repository ID. */
  public RmiIiopWireUserException(String repositoryId) {
    this(repositoryId, List.of());
  }

  /** Creates a wire user-exception marker for a declared repository ID and payload fields. */
  public RmiIiopWireUserException(String repositoryId, List<RmiCdrValue> fields) {
    super("Remote user exception: " + requireNonBlank(repositoryId, "repositoryId"));
    this.repositoryId = repositoryId;
    this.fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
  }

  /** Returns the user-exception repository ID read from the reply body. */
  public String repositoryId() {
    return repositoryId;
  }

  /** Returns decoded user-exception field values. */
  public List<RmiCdrValue> fields() {
    return fields;
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
