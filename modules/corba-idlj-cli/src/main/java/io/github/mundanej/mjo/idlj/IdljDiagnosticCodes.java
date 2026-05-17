package io.github.mundanej.mjo.idlj;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes emitted by the idlj command-line wrapper. */
public final class IdljDiagnosticCodes {

  /** Command-line arguments are missing or not valid for the requested command. */
  public static final DiagnosticCode INVALID_ARGUMENTS = new DiagnosticCode("IDLJ-0001");

  /** A root IDL source file could not be read. */
  public static final DiagnosticCode SOURCE_READ_FAILED = new DiagnosticCode("IDLJ-0002");

  private IdljDiagnosticCodes() {}
}
