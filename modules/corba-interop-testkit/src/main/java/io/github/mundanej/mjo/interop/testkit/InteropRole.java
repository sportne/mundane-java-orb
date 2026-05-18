package io.github.mundanej.mjo.interop.testkit;

/** Peer container role used by the interop harness. */
public enum InteropRole {
  CLIENT("client"),
  SERVER("server"),
  NAMING("naming"),
  HEALTH("health"),
  REPORT("report");

  private final String wireName;

  InteropRole(String wireName) {
    this.wireName = wireName;
  }

  public String wireName() {
    return wireName;
  }

  public static InteropRole fromWireName(String wireName) {
    for (InteropRole role : values()) {
      if (role.wireName.equals(wireName)) {
        return role;
      }
    }
    throw new IllegalArgumentException("unknown interop role: " + wireName);
  }
}
