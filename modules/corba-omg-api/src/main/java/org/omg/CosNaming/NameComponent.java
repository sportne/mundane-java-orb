package org.omg.CosNaming;

/** CosNaming name component. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class NameComponent implements org.omg.CORBA.portable.IDLEntity {

  /** Component identifier. */
  public String id;

  /** Component kind. */
  public String kind;

  /** Creates an empty name component. */
  public NameComponent() {}

  /** Creates a name component. */
  public NameComponent(String id, String kind) {
    this.id = id;
    this.kind = kind;
  }

  @Override
  public String toString() {
    return "NameComponent[id=" + id + ", kind=" + kind + ']';
  }
}
