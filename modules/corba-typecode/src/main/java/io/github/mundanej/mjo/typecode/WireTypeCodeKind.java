package io.github.mundanej.mjo.typecode;

/** CORBA TCKind values used by the G10 wire TypeCode codec. */
public enum WireTypeCodeKind {
  NULL(0),
  VOID(1),
  SHORT(2),
  LONG(3),
  UNSIGNED_SHORT(4),
  UNSIGNED_LONG(5),
  FLOAT(6),
  DOUBLE(7),
  BOOLEAN(8),
  CHAR(9),
  OCTET(10),
  ANY(11),
  TYPECODE(12),
  OBJECT_REFERENCE(14),
  STRUCT(15),
  UNION(16),
  ENUM(17),
  STRING(18),
  SEQUENCE(19),
  ARRAY(20),
  ALIAS(21),
  EXCEPTION(22),
  LONG_LONG(23),
  UNSIGNED_LONG_LONG(24),
  LONG_DOUBLE(25),
  WCHAR(26),
  WSTRING(27);

  private final int id;

  WireTypeCodeKind(int id) {
    this.id = id;
  }

  /** Returns the standard unsigned-long TCKind value. */
  public int id() {
    return id;
  }

  /** Maps a TCKind unsigned-long value to a supported kind. */
  public static WireTypeCodeKind fromId(long id) {
    for (WireTypeCodeKind kind : values()) {
      if (kind.id == id) {
        return kind;
      }
    }
    throw new IllegalArgumentException("unsupported wire TypeCode kind: " + id);
  }
}
