package io.github.mundanej.mjo.typecode;

/** Supported local TypeCode kinds for descriptor-backed dynamic values. */
public enum IdlTypeCodeKind {
  VOID,
  BOOLEAN,
  OCTET,
  CHAR,
  SHORT,
  UNSIGNED_SHORT,
  LONG,
  UNSIGNED_LONG,
  LONG_LONG,
  UNSIGNED_LONG_LONG,
  FLOAT,
  DOUBLE,
  LONG_DOUBLE,
  STRING,
  INTERFACE,
  STRUCT,
  ENUM,
  EXCEPTION,
  TYPEDEF,
  UNION,
  NATIVE,
  VALUE_BOX,
  VALUETYPE,
  SEQUENCE
}
