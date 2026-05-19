package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * Root Java-to-IDL mapping model for one Java remote interface declaration.
 *
 * @param modules top-level IDL modules in deterministic order
 * @param interfaces top-level IDL interfaces for default-package Java declarations
 */
public record RmiIdlTranslationUnit(List<RmiIdlModule> modules, List<RmiIdlInterface> interfaces) {

  /** Creates an immutable translation-unit model. */
  public RmiIdlTranslationUnit {
    modules = List.copyOf(Objects.requireNonNull(modules, "modules"));
    interfaces = List.copyOf(Objects.requireNonNull(interfaces, "interfaces"));
  }
}
