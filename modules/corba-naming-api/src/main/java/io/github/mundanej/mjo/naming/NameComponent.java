package io.github.mundanej.mjo.naming;

import java.util.Objects;

/**
 * One CosNaming name component.
 *
 * @param id component id, possibly empty when kind is present
 * @param kind component kind, possibly empty when id is present
 */
public record NameComponent(String id, String kind) {

  /** Creates a validated name component. */
  public NameComponent {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(kind, "kind");
    if (id.isEmpty() && kind.isEmpty()) {
      throw new NamingException(
          NamingDiagnosticCodes.INVALID_NAME, "name component must have id or kind");
    }
  }

  /** Creates a component with an empty kind. */
  public static NameComponent id(String id) {
    return new NameComponent(id, "");
  }
}
