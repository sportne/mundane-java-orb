package io.github.mundanej.mjo.interop.testkit;

/** Stable status values used by structured interop reports. */
public enum InteropReportStatus {
  PASSED("passed"),
  FAILED("failed"),
  SKIPPED("skipped");

  private final String wireName;

  InteropReportStatus(String wireName) {
    this.wireName = wireName;
  }

  public String wireName() {
    return wireName;
  }

  public static InteropReportStatus fromWireName(String wireName) {
    for (InteropReportStatus status : values()) {
      if (status.wireName.equals(wireName)) {
        return status;
      }
    }
    throw new IllegalArgumentException("unknown interop report status: " + wireName);
  }
}
