package org.omg.DynamicAny;

import java.util.Objects;

/** Name and DynAny value pair. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class NameDynAnyPair {

  public String id;
  public DynAny value;

  /** Creates an empty pair. */
  public NameDynAnyPair() {}

  /** Creates a pair. */
  public NameDynAnyPair(String id, DynAny value) {
    this.id = id;
    this.value = value;
  }

  @Override
  public String toString() {
    return "NameDynAnyPair[id=" + id + ", value=" + Objects.toString(value) + ']';
  }
}
