package io.github.mundanej.mjo.idlj;

/** Stable process exit codes for the idlj command-line tool. */
public final class IdljExitCodes {

  /** Validation completed without error diagnostics. */
  public static final int SUCCESS = 0;

  /** Source validation completed and emitted one or more error diagnostics. */
  public static final int VALIDATION_FAILED = 1;

  /** Command-line usage or source input failed before validation could complete. */
  public static final int USAGE_OR_INPUT_ERROR = 2;

  private IdljExitCodes() {}
}
