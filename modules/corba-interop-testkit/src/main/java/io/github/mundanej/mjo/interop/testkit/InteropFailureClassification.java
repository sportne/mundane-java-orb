package io.github.mundanej.mjo.interop.testkit;

/** Stable clean-room classification values for interop outcomes. */
public enum InteropFailureClassification {
  OUR_BUG("our-bug"),
  PEER_BUG("peer-bug"),
  SPEC_AMBIGUITY("spec-ambiguity"),
  PROFILE_MISMATCH("profile-mismatch"),
  INFRASTRUCTURE_FAILURE("infrastructure-failure"),
  MISSING_PREREQUISITE("missing-prerequisite"),
  UNSUPPORTED_SCENARIO("unsupported-scenario"),
  EXPECTED_DEFERRAL("expected-deferral"),
  SERVER_READY("server-ready"),
  OBJECT_REFERENCE_CHECKED("object-reference-checked"),
  CALCULATOR_CHECKED("calculator-checked"),
  TIME_SERVICE_CHECKED("time-service-checked"),
  DURABLE_IOR_INVOKED("durable-ior-invoked"),
  DURABLE_NAMING_RESOLVED("durable-naming-resolved");

  private final String wireName;

  InteropFailureClassification(String wireName) {
    this.wireName = wireName;
  }

  public String wireName() {
    return wireName;
  }

  public static InteropFailureClassification fromWireName(String wireName) {
    for (InteropFailureClassification classification : values()) {
      if (classification.wireName.equals(wireName)) {
        return classification;
      }
    }
    throw new IllegalArgumentException("unknown interop failure classification: " + wireName);
  }
}
