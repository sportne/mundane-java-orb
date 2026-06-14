package io.github.mundanej.mjo.interop.testkit;

/** Immutable scenario identity and IDL corpus path for an interop run. */
public record InteropScenario(String name, String idlPath) {
  public InteropScenario {
    name = requireScenarioName(name);
    idlPath = requireRelativeIdlPath(idlPath);
  }

  /** Returns the approved G7 RMI-IIOP peer scenario. */
  public static InteropScenario rmiIiop() {
    return new InteropScenario("rmi-iiop", "interop/idl/rmi-iiop/Calculator.idl");
  }

  /** Returns the approved G8 Event Service peer metadata scenario. */
  public static InteropScenario eventService() {
    return new InteropScenario("event-service", "interop/idl/event-service.idl");
  }

  /** Returns the approved G8 Notification Service peer metadata scenario. */
  public static InteropScenario notificationService() {
    return new InteropScenario("notification-service", "interop/idl/notification-service.idl");
  }

  private static String requireScenarioName(String value) {
    requireNotBlank(value, "name");
    if (!value.matches("[A-Za-z0-9._-]+")) {
      throw new IllegalArgumentException(
          "name must contain only letters, digits, dot, underscore, or dash");
    }
    return value;
  }

  private static String requireRelativeIdlPath(String value) {
    requireNotBlank(value, "idlPath");
    if (value.startsWith("/") || value.contains("..")) {
      throw new IllegalArgumentException("idlPath must be a contained relative path");
    }
    return value;
  }

  private static void requireNotBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
