package org.omg.CosNaming.NamingContextPackage;

/** Enum-like CosNaming not-found reason. */
public final class NotFoundReason {

  public static final int _missing_node = 0;
  public static final int _not_context = 1;
  public static final int _not_object = 2;

  public static final NotFoundReason missing_node = new NotFoundReason(_missing_node);
  public static final NotFoundReason not_context = new NotFoundReason(_not_context);
  public static final NotFoundReason not_object = new NotFoundReason(_not_object);

  private final int value;

  private NotFoundReason(int value) {
    this.value = value;
  }

  /** Returns the integer constant value. */
  public int value() {
    return value;
  }
}
