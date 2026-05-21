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

  /** A sequence type is outside the generated IDL fixture slice. */
  public static final DiagnosticCode UNSUPPORTED_GENERATED_IDL_SEQUENCE =
      new DiagnosticCode("RMI-0400");

  /** A declared value/reference type is outside the generated IDL fixture slice. */
  public static final DiagnosticCode UNSUPPORTED_GENERATED_IDL_DECLARED_TYPE =
      new DiagnosticCode("RMI-0401");

  /** An exception reference cannot be declared in the fixture slice. */
  public static final DiagnosticCode UNSUPPORTED_GENERATED_IDL_EXCEPTION_SCOPE =
      new DiagnosticCode("RMI-0402");

  /** A generated Java binding surface is missing required repository ID metadata. */
  public static final DiagnosticCode MISSING_BINDING_REPOSITORY_ID = new DiagnosticCode("RMI-0500");

  /** A sequence type is outside the generated Java binding slice. */
  public static final DiagnosticCode UNSUPPORTED_BINDING_SEQUENCE = new DiagnosticCode("RMI-0501");

  /** A declared value/reference type is outside the generated Java binding slice. */
  public static final DiagnosticCode UNSUPPORTED_BINDING_DECLARED_TYPE =
      new DiagnosticCode("RMI-0502");

  /** An exception reference cannot be emitted in the generated Java binding slice. */
  public static final DiagnosticCode UNSUPPORTED_BINDING_EXCEPTION_SCOPE =
      new DiagnosticCode("RMI-0503");

  /** Two generated Java binding sources resolved to the same source path. */
  public static final DiagnosticCode DUPLICATE_BINDING_SOURCE_PATH = new DiagnosticCode("RMI-0504");

  /** An IDL type reference is outside the local RMI CDR marshaling slice. */
  public static final DiagnosticCode UNSUPPORTED_CDR_MARSHALING_TYPE =
      new DiagnosticCode("RMI-0600");

  /** A supplied value does not match the expected RMI CDR value kind or Java type. */
  public static final DiagnosticCode CDR_VALUE_TYPE_MISMATCH = new DiagnosticCode("RMI-0601");

  /** Null values are outside the local RMI CDR marshaling slice. */
  public static final DiagnosticCode CDR_NULL_VALUE = new DiagnosticCode("RMI-0602");

  /** Operation arguments did not match the declared parameter count. */
  public static final DiagnosticCode CDR_OPERATION_ARGUMENT_COUNT_MISMATCH =
      new DiagnosticCode("RMI-0603");

  /** A declared user exception is missing required repository ID metadata. */
  public static final DiagnosticCode CDR_MISSING_EXCEPTION_REPOSITORY_ID =
      new DiagnosticCode("RMI-0604");

  /** A user exception repository ID was not declared for the operation. */
  public static final DiagnosticCode CDR_UNDECLARED_EXCEPTION_REPOSITORY_ID =
      new DiagnosticCode("RMI-0605");

  /** A wire object key is blank, malformed, or exceeds the approved bound. */
  public static final DiagnosticCode INVALID_WIRE_OBJECT_KEY = new DiagnosticCode("RMI-0800");

  /** No wire binding exists for the requested object key. */
  public static final DiagnosticCode UNKNOWN_WIRE_OBJECT_KEY = new DiagnosticCode("RMI-0801");

  /** No operation is declared for the requested wire operation name. */
  public static final DiagnosticCode UNKNOWN_WIRE_OPERATION = new DiagnosticCode("RMI-0802");

  /** A wire request or reply body is malformed for the approved RMI-IIOP slice. */
  public static final DiagnosticCode MALFORMED_WIRE_BODY = new DiagnosticCode("RMI-0803");

  /** A GIOP reply status is outside the approved RMI-IIOP wire slice. */
  public static final DiagnosticCode UNSUPPORTED_WIRE_REPLY_STATUS = new DiagnosticCode("RMI-0804");

  /** A remote system-exception reply was received. */
  public static final DiagnosticCode REMOTE_SYSTEM_EXCEPTION_REPLY = new DiagnosticCode("RMI-0805");

  /** A wire user-exception repository ID was not declared for the operation. */
  public static final DiagnosticCode UNDECLARED_WIRE_USER_EXCEPTION =
      new DiagnosticCode("RMI-0806");

  private RmiJavaDiagnosticCodes() {}
}
