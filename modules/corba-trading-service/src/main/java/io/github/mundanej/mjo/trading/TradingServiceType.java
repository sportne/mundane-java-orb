package io.github.mundanej.mjo.trading;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable service type metadata for the supported local Trading Service subset. */
public final class TradingServiceType {

  private final String name;
  private final List<TradingPropertyDefinition> properties;

  /** Creates a validated service type using maximum supported standalone model limits. */
  public TradingServiceType(String name, List<TradingPropertyDefinition> properties) {
    this.name =
        TradingNames.requireName(name, "service type name", TradingServiceOptions.modelLimits());
    this.properties = validatedProperties(properties, TradingServiceOptions.modelLimits());
  }

  /** Creates a service type with no property definitions. */
  public static TradingServiceType empty(String name) {
    return new TradingServiceType(name, List.of());
  }

  /** Returns the service type name. */
  public String name() {
    return name;
  }

  /** Returns an immutable snapshot of property definitions. */
  public List<TradingPropertyDefinition> properties() {
    return List.copyOf(properties);
  }

  static TradingServiceType validateForRepository(
      TradingServiceType type, TradingServiceOptions options) {
    if (type == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_TYPE, "service type must not be null");
    }
    return new TradingServiceType(
        TradingNames.requireName(type.name(), "service type name", options),
        validatedProperties(type.properties(), options));
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TradingServiceType that)) {
      return false;
    }
    return name.equals(that.name) && properties.equals(that.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, properties);
  }

  @Override
  public String toString() {
    return "TradingServiceType[name=" + name + ", properties=" + properties + "]";
  }

  private static List<TradingPropertyDefinition> validatedProperties(
      List<TradingPropertyDefinition> definitions, TradingServiceOptions options) {
    if (definitions == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_TYPE, "property definitions must not be null");
    }
    if (definitions.size() > options.maxPropertiesPerType()) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.PROPERTY_LIMIT_EXCEEDED,
          "service type exceeds " + options.maxPropertiesPerType() + " property definitions");
    }
    Set<String> names = new HashSet<>();
    List<TradingPropertyDefinition> copy = new ArrayList<>(definitions.size());
    for (TradingPropertyDefinition definition : definitions) {
      if (definition == null) {
        throw new TradingServiceException(
            TradingServiceDiagnosticCodes.MALFORMED_TYPE, "property definition must not be null");
      }
      String propertyName = TradingNames.requireName(definition.name(), "property name", options);
      if (!names.add(propertyName)) {
        throw new TradingServiceException(
            TradingServiceDiagnosticCodes.DUPLICATE_PROPERTY,
            "duplicate property definition: " + propertyName);
      }
      copy.add(
          new TradingPropertyDefinition(propertyName, definition.kind(), definition.required()));
    }
    return List.copyOf(copy);
  }
}
