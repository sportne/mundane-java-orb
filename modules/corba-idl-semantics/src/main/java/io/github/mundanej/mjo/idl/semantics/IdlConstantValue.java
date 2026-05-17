package io.github.mundanej.mjo.idl.semantics;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** Type-safe evaluated value for the constant subset implemented by G6-140. */
public sealed interface IdlConstantValue
    permits IdlConstantValue.BooleanValue,
        IdlConstantValue.CharacterValue,
        IdlConstantValue.EnumeratorValue,
        IdlConstantValue.FloatingValue,
        IdlConstantValue.IntegerValue,
        IdlConstantValue.StringValue {

  /** Evaluated constant value categories. */
  enum Kind {
    /** Exact integer value. */
    INTEGER,
    /** Decimal floating value. */
    FLOATING,
    /** Boolean value. */
    BOOLEAN,
    /** Character or wide-character value. */
    CHARACTER,
    /** String or wide-string value. */
    STRING,
    /** Enum value reference. */
    ENUMERATOR
  }

  /** Returns the value category. */
  Kind kind();

  /** Returns the IDL type name used for this value. */
  String idlType();

  /** Creates an integer constant value. */
  static IntegerValue integer(String idlType, BigInteger value) {
    return new IntegerValue(idlType, value);
  }

  /** Creates a floating constant value. */
  static FloatingValue floating(String idlType, BigDecimal value) {
    return new FloatingValue(idlType, value);
  }

  /** Creates a boolean constant value. */
  static BooleanValue bool(String idlType, boolean value) {
    return new BooleanValue(idlType, value);
  }

  /** Creates a character constant value. */
  static CharacterValue character(String idlType, String value) {
    return new CharacterValue(idlType, value);
  }

  /** Creates a string constant value. */
  static StringValue string(String idlType, String value) {
    return new StringValue(idlType, value);
  }

  /** Creates an enum constant value. */
  static EnumeratorValue enumerator(String idlType, String enumeratorName) {
    return new EnumeratorValue(idlType, enumeratorName);
  }

  /** Exact integer constant value. */
  record IntegerValue(String idlType, BigInteger value) implements IdlConstantValue {

    /** Creates a validated integer constant value. */
    public IntegerValue {
      idlType = requireNonBlank(idlType, "idlType");
      Objects.requireNonNull(value, "value");
    }

    @Override
    public Kind kind() {
      return Kind.INTEGER;
    }
  }

  /** Decimal floating constant value. */
  record FloatingValue(String idlType, BigDecimal value) implements IdlConstantValue {

    /** Creates a validated floating constant value. */
    public FloatingValue {
      idlType = requireNonBlank(idlType, "idlType");
      Objects.requireNonNull(value, "value");
    }

    @Override
    public Kind kind() {
      return Kind.FLOATING;
    }
  }

  /** Boolean constant value. */
  record BooleanValue(String idlType, boolean value) implements IdlConstantValue {

    /** Creates a validated boolean constant value. */
    public BooleanValue {
      idlType = requireNonBlank(idlType, "idlType");
    }

    @Override
    public Kind kind() {
      return Kind.BOOLEAN;
    }
  }

  /** Character constant value. */
  record CharacterValue(String idlType, String value) implements IdlConstantValue {

    /** Creates a validated character constant value. */
    public CharacterValue {
      idlType = requireNonBlank(idlType, "idlType");
      Objects.requireNonNull(value, "value");
      if (value.isEmpty()) {
        throw new IllegalArgumentException("value must not be empty");
      }
    }

    @Override
    public Kind kind() {
      return Kind.CHARACTER;
    }
  }

  /** String constant value. */
  record StringValue(String idlType, String value) implements IdlConstantValue {

    /** Creates a validated string constant value. */
    public StringValue {
      idlType = requireNonBlank(idlType, "idlType");
      Objects.requireNonNull(value, "value");
    }

    @Override
    public Kind kind() {
      return Kind.STRING;
    }
  }

  /** Enum constant value. */
  record EnumeratorValue(String idlType, String enumeratorName) implements IdlConstantValue {

    /** Creates a validated enum constant value. */
    public EnumeratorValue {
      idlType = requireNonBlank(idlType, "idlType");
      enumeratorName = requireNonBlank(enumeratorName, "enumeratorName");
    }

    @Override
    public Kind kind() {
      return Kind.ENUMERATOR;
    }
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
