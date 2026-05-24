package org.omg.CORBA;

import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;

/** API-only CORBA TypeCode compatibility surface. */
public abstract class TypeCode {

  /** Returns the TypeCode kind. */
  public abstract TCKind kind();

  /** Returns the repository ID for named TypeCodes. */
  public abstract String id() throws BadKind;

  /** Returns the simple IDL name for named TypeCodes. */
  public abstract String name() throws BadKind;

  /** Returns the member count. */
  public abstract int member_count() throws BadKind;

  /** Returns the member name at the given index. */
  public abstract String member_name(int index) throws BadKind, Bounds;

  /** Returns the member type at the given index. */
  public abstract TypeCode member_type(int index) throws BadKind, Bounds;

  /** Returns whether this TypeCode is equivalent to another TypeCode. */
  public abstract boolean equivalent(TypeCode other);

  /** Returns a TypeCode with aliases removed. */
  public abstract TypeCode get_compact_typecode();
}
