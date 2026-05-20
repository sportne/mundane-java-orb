package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes emitted by RMI-IIOP Java eligibility checks. */
public final class RmiJavaDiagnosticCodes {

  /** No declaration was provided to the eligibility checker. */
  public static final DiagnosticCode NULL_DECLARATION = new DiagnosticCode("RMI-0100");

  /** The remote interface binary name is blank or not a valid Java binary name. */
  public static final DiagnosticCode INVALID_INTERFACE_NAME = new DiagnosticCode("RMI-0101");

  /** The declaration is not marked as a java.rmi.Remote-compatible interface. */
  public static final DiagnosticCode NON_REMOTE_INTERFACE = new DiagnosticCode("RMI-0102");

  /** An operation name is blank, reserved, or not a valid Java identifier. */
  public static final DiagnosticCode INVALID_OPERATION_NAME = new DiagnosticCode("RMI-0103");

  /** An operation uses a method kind outside the approved abstract-method slice. */
  public static final DiagnosticCode UNSUPPORTED_OPERATION_KIND = new DiagnosticCode("RMI-0104");

  /** An operation uses Java varargs, which are deferred to a later mapping task. */
  public static final DiagnosticCode UNSUPPORTED_VARARGS = new DiagnosticCode("RMI-0105");

  /** An operation name appears more than once; overload mapping is deferred. */
  public static final DiagnosticCode DUPLICATE_OPERATION_NAME = new DiagnosticCode("RMI-0106");

  /** A return, parameter, or declared type reference is outside the approved slice. */
  public static final DiagnosticCode UNSUPPORTED_TYPE_REFERENCE = new DiagnosticCode("RMI-0107");

  /** An operation does not explicitly declare java.rmi.RemoteException. */
  public static final DiagnosticCode MISSING_REMOTE_EXCEPTION = new DiagnosticCode("RMI-0108");

  /** A parameter name is blank, reserved, or not a valid Java identifier. */
  public static final DiagnosticCode INVALID_PARAMETER_NAME = new DiagnosticCode("RMI-0109");

  /** An exception type reference is outside the approved explicit-name slice. */
  public static final DiagnosticCode INVALID_EXCEPTION_TYPE = new DiagnosticCode("RMI-0110");

  /** A Java type shape cannot be mapped by the approved Java-to-IDL model slice. */
  public static final DiagnosticCode UNSUPPORTED_IDL_TYPE_MAPPING = new DiagnosticCode("RMI-0200");

  /** A Java package segment cannot be used as an IDL module identifier. */
  public static final DiagnosticCode INVALID_IDL_MODULE_NAME = new DiagnosticCode("RMI-0201");

  /** A Java interface simple name cannot be used as an IDL interface identifier. */
  public static final DiagnosticCode INVALID_IDL_INTERFACE_NAME = new DiagnosticCode("RMI-0202");

  /** A Java method name cannot be used as an IDL operation identifier. */
  public static final DiagnosticCode INVALID_IDL_OPERATION_NAME = new DiagnosticCode("RMI-0203");

  /** A Java parameter name cannot be used as an IDL parameter identifier. */
  public static final DiagnosticCode INVALID_IDL_PARAMETER_NAME = new DiagnosticCode("RMI-0204");

  /** A Java checked exception name cannot be used as an IDL exception scoped name. */
  public static final DiagnosticCode INVALID_IDL_EXCEPTION_NAME = new DiagnosticCode("RMI-0205");

  /** Repository ID hash metadata is missing for a Java binary name. */
  public static final DiagnosticCode MISSING_REPOSITORY_ID_HASH = new DiagnosticCode("RMI-0300");

  /** More than one repository ID hash metadata entry was provided for the same name. */
  public static final DiagnosticCode DUPLICATE_REPOSITORY_ID_HASH = new DiagnosticCode("RMI-0301");

  /** Repository ID hash metadata uses an invalid Java binary name. */
  public static final DiagnosticCode INVALID_REPOSITORY_ID_NAME = new DiagnosticCode("RMI-0302");

  /** Repository ID hash metadata uses an invalid 16-hex-digit hash. */
  public static final DiagnosticCode INVALID_REPOSITORY_ID_HASH = new DiagnosticCode("RMI-0303");

  /** Repository ID hash metadata uses an invalid 16-hex-digit serialVersionUID. */
  public static final DiagnosticCode INVALID_REPOSITORY_ID_UID = new DiagnosticCode("RMI-0304");

  /** Java-to-IDL model data does not contain the Java binary-name metadata needed for RMI IDs. */
  public static final DiagnosticCode UNRESOLVED_REPOSITORY_ID_MODEL_NAME =
      new DiagnosticCode("RMI-0305");

  private RmiJavaDiagnosticCodes() {}
}
