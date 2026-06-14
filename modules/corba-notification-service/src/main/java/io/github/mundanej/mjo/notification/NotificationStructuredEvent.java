package io.github.mundanej.mjo.notification;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Immutable structured event for the supported local Notification Service subset. */
public record NotificationStructuredEvent(
    NotificationEventIdentity identity,
    List<NotificationProperty> filterProperties,
    List<NotificationProperty> variableHeaderFields,
    List<NotificationProperty> bodyFields) {

  /** Maximum supported characters for identity fields and property names. */
  public static final int MAX_NAME_LENGTH = 128;

  /** Maximum supported fields in one structured-event section. */
  public static final int MAX_FIELDS_PER_SECTION = 64;

  /** Creates a validated immutable structured event. */
  public NotificationStructuredEvent {
    identity = requirePresent("identity", identity);
    filterProperties = validateSection("filterProperties", filterProperties);
    variableHeaderFields = validateSection("variableHeaderFields", variableHeaderFields);
    bodyFields = validateSection("bodyFields", bodyFields);
  }

  /** Creates an event with no optional structured-event sections. */
  public static NotificationStructuredEvent of(NotificationEventIdentity identity) {
    return new NotificationStructuredEvent(identity, List.of(), List.of(), List.of());
  }

  /** Returns a defensive copy of filterable primitive properties. */
  @Override
  public List<NotificationProperty> filterProperties() {
    return List.copyOf(filterProperties);
  }

  /** Returns a defensive copy of variable header fields. */
  @Override
  public List<NotificationProperty> variableHeaderFields() {
    return List.copyOf(variableHeaderFields);
  }

  /** Returns a defensive copy of body fields. */
  @Override
  public List<NotificationProperty> bodyFields() {
    return List.copyOf(bodyFields);
  }

  static String requireName(String value, String name) {
    requirePresent(name, value);
    if (value.isBlank()) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT,
          name + " must not be blank");
    }
    if (value.length() > MAX_NAME_LENGTH) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT,
          name + " exceeds " + MAX_NAME_LENGTH + " characters");
    }
    return value;
  }

  static <T> T requirePresent(String name, T value) {
    if (value == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT,
          name + " must not be null");
    }
    return value;
  }

  private static List<NotificationProperty> validateSection(
      String sectionName, List<NotificationProperty> properties) {
    requirePresent(sectionName, properties);
    if (properties.size() > MAX_FIELDS_PER_SECTION) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.FIELD_LIMIT_EXCEEDED,
          sectionName + " exceeds " + MAX_FIELDS_PER_SECTION + " fields");
    }
    Set<String> names = new LinkedHashSet<>();
    for (NotificationProperty property : properties) {
      NotificationProperty checked = requirePresent(sectionName + " property", property);
      if (!names.add(checked.name())) {
        throw new NotificationServiceException(
            NotificationServiceDiagnosticCodes.DUPLICATE_FIELD,
            sectionName + " repeats property name: " + checked.name());
      }
    }
    return List.copyOf(properties);
  }
}
