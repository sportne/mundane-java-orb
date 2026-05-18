package io.github.mundanej.mjo.ir;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for local static Interface Repository failures. */
public final class InterfaceRepositoryDiagnosticCodes {

  /** A descriptor key was duplicated while building a static repository. */
  public static final DiagnosticCode DUPLICATE_DESCRIPTOR = new DiagnosticCode("IR-0001");

  /** A requested descriptor or operation was not present in the repository. */
  public static final DiagnosticCode MISSING_DESCRIPTOR = new DiagnosticCode("IR-0002");

  /** A generated type reference could not be resolved through the repository. */
  public static final DiagnosticCode INVALID_REFERENCE = new DiagnosticCode("IR-0003");

  /** A descriptor kind is outside the static Interface Repository slice. */
  public static final DiagnosticCode UNSUPPORTED_DESCRIPTOR_KIND = new DiagnosticCode("IR-0004");

  private InterfaceRepositoryDiagnosticCodes() {}
}
