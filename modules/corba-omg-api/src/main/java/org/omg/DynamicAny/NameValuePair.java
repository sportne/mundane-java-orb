package org.omg.DynamicAny;

import java.util.Objects;

/** Name and Any value pair. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class NameValuePair {

  public String id;
  public org.omg.CORBA.Any value;

  /** Creates an empty pair. */
  public NameValuePair() {}

  /** Creates a pair. */
  public NameValuePair(String id, org.omg.CORBA.Any value) {
    this.id = id;
    this.value = value;
  }

  @Override
  public String toString() {
    return "NameValuePair[id=" + id + ", value=" + Objects.toString(value) + ']';
  }
}
