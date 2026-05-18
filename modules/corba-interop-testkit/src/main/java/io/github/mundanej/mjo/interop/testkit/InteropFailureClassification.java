package io.github.mundanej.mjo.interop.testkit;

/** Stable clean-room classification values for interop outcomes. */
public enum InteropFailureClassification {
  OUR_BUG("our-bug"),
  PEER_BUG("peer-bug"),
  SPEC_AMBIGUITY("spec-ambiguity"),
  PROFILE_MISMATCH("profile-mismatch"),
  INFRASTRUCTURE_FAILURE("infrastructure-failure"),
  EXPECTED_DEFERRAL("expected-deferral");

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
