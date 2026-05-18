package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for local dynamic CORBA failures. */
public final class DynamicDiagnosticCodes {

  /** A dynamic value or argument did not match its expected TypeCode. */
  public static final DiagnosticCode TYPE_MISMATCH = new DiagnosticCode("DYN-0001");

  /** A parameter mode is outside the current IN-only dynamic invocation subset. */
  public static final DiagnosticCode UNSUPPORTED_PARAMETER_MODE = new DiagnosticCode("DYN-0002");

  /** A dynamic request named an operation not present in the configured descriptor set. */
  public static final DiagnosticCode UNKNOWN_OPERATION = new DiagnosticCode("DYN-0003");

  /** A dynamic request supplied invalid argument counts or values. */
  public static final DiagnosticCode INVALID_ARGUMENTS = new DiagnosticCode("DYN-0004");

  /** The requested TypeCode kind is outside the local DynamicAny slice. */
  public static final DiagnosticCode UNSUPPORTED_TYPE = new DiagnosticCode("DYN-0005");

  /** A declared user exception was raised through dynamic invocation. */
  public static final DiagnosticCode USER_EXCEPTION = new DiagnosticCode("DYN-0006");

  private DynamicDiagnosticCodes() {}
}
