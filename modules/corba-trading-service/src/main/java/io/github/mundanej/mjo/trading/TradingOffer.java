package io.github.mundanej.mjo.trading;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable offer metadata for the supported local Trading Service subset. */
public final class TradingOffer {

  private final String id;
  private final String typeName;
  private final Map<String, Object> properties;

  /** Creates a validated offer using maximum supported standalone model limits. */
  public TradingOffer(String id, String typeName, Map<String, ?> properties) {
    this.id = TradingNames.requireName(id, "offer ID", TradingServiceOptions.modelLimits());
    this.typeName =
        TradingNames.requireName(
            typeName, "offer service type name", TradingServiceOptions.modelLimits());
    this.properties = validatedProperties(properties, TradingServiceOptions.modelLimits());
  }

  /** Creates an offer with no property values. */
  public static TradingOffer empty(String id, String typeName) {
    return new TradingOffer(id, typeName, Map.of());
  }

  /** Returns the offer ID. */
  public String id() {
    return id;
  }

  /** Returns the referenced service type name. */
  public String typeName() {
    return typeName;
  }

  /** Returns an immutable property-value snapshot. */
  public Map<String, Object> properties() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(properties));
  }

  static TradingOffer validateForRepository(
      TradingOffer offer,
      TradingServiceType type,
      TradingServiceOptions typeOptions,
      TradingOfferRepositoryOptions offerOptions) {
    if (offer == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_OFFER, "offer must not be null");
    }
    String offerId =
        TradingNames.requireBoundedText(offer.id(), "offer ID", offerOptions.maxOfferIdLength());
    String offerTypeName =
        TradingNames.requireName(offer.typeName(), "offer service type", typeOptions);
    if (!type.name().equals(offerTypeName)) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.PROPERTY_MISMATCH,
          "offer type " + offerTypeName + " does not match resolved type " + type.name());
    }
    Map<String, Object> validated =
        validatePropertiesForType(offer.properties(), type, typeOptions, offerOptions);
    return new TradingOffer(offerId, offerTypeName, validated);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TradingOffer that)) {
      return false;
    }
    return id.equals(that.id)
        && typeName.equals(that.typeName)
        && properties.equals(that.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, typeName, properties);
  }

  @Override
  public String toString() {
    return "TradingOffer[id=" + id + ", typeName=" + typeName + ", properties=" + properties + "]";
  }

  private static Map<String, Object> validatedProperties(
      Map<String, ?> values, TradingServiceOptions options) {
    if (values == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_OFFER, "offer properties must not be null");
    }
    Map<String, Object> copy = new LinkedHashMap<>();
    for (Map.Entry<String, ?> entry : values.entrySet()) {
      String propertyName =
          TradingNames.requireName(entry.getKey(), "offer property name", options);
      copy.put(propertyName, entry.getValue());
    }
    return Collections.unmodifiableMap(copy);
  }

  private static Map<String, Object> validatePropertiesForType(
      Map<String, Object> values,
      TradingServiceType type,
      TradingServiceOptions typeOptions,
      TradingOfferRepositoryOptions offerOptions) {
    if (values.size() > offerOptions.maxPropertiesPerOffer()) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.PROPERTY_LIMIT_EXCEEDED,
          "offer exceeds " + offerOptions.maxPropertiesPerOffer() + " property values");
    }
    Map<String, TradingPropertyDefinition> definitions = new LinkedHashMap<>();
    for (TradingPropertyDefinition definition : type.properties()) {
      definitions.put(definition.name(), definition);
    }

    Map<String, Object> validated = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      String propertyName =
          TradingNames.requireName(entry.getKey(), "offer property name", typeOptions);
      TradingPropertyDefinition definition = definitions.get(propertyName);
      if (definition == null) {
        throw new TradingServiceException(
            TradingServiceDiagnosticCodes.PROPERTY_MISMATCH,
            "offer property is not defined by service type: " + propertyName);
      }
      validated.put(
          propertyName,
          validatedValue(propertyName, definition.kind(), entry.getValue(), offerOptions));
    }
    for (TradingPropertyDefinition definition : definitions.values()) {
      if (definition.required() && !validated.containsKey(definition.name())) {
        throw new TradingServiceException(
            TradingServiceDiagnosticCodes.PROPERTY_MISMATCH,
            "offer is missing required property: " + definition.name());
      }
    }
    return validated;
  }

  private static Object validatedValue(
      String propertyName,
      TradingPrimitiveKind kind,
      Object value,
      TradingOfferRepositoryOptions options) {
    if (value == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.UNSUPPORTED_VALUE,
          "offer property value must not be null: " + propertyName);
    }
    return switch (kind) {
      case STRING -> validatedString(propertyName, value, options);
      case BOOLEAN -> validatedBoolean(propertyName, value);
      case SIGNED_LONG -> validatedSignedLong(propertyName, value);
      case FLOATING_POINT -> validatedFloatingPoint(propertyName, value);
    };
  }

  private static String validatedString(
      String propertyName, Object value, TradingOfferRepositoryOptions options) {
    if (!(value instanceof String text)) {
      throw propertyMismatch(propertyName, TradingPrimitiveKind.STRING);
    }
    if (text.length() > options.maxStringValueLength()) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.VALUE_LIMIT_EXCEEDED,
          "string property exceeds "
              + options.maxStringValueLength()
              + " characters: "
              + propertyName);
    }
    return text;
  }

  private static Boolean validatedBoolean(String propertyName, Object value) {
    if (!(value instanceof Boolean bool)) {
      throw propertyMismatch(propertyName, TradingPrimitiveKind.BOOLEAN);
    }
    return bool;
  }

  private static Long validatedSignedLong(String propertyName, Object value) {
    if (!(value instanceof Long number)) {
      throw propertyMismatch(propertyName, TradingPrimitiveKind.SIGNED_LONG);
    }
    return number;
  }

  private static Double validatedFloatingPoint(String propertyName, Object value) {
    if (!(value instanceof Double number) || !Double.isFinite(number)) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.UNSUPPORTED_VALUE,
          "floating-point property must be a finite double: " + propertyName);
    }
    return number;
  }

  private static TradingServiceException propertyMismatch(
      String propertyName, TradingPrimitiveKind expected) {
    return new TradingServiceException(
        TradingServiceDiagnosticCodes.PROPERTY_MISMATCH,
        "offer property " + propertyName + " must be " + expected);
  }
}
