package io.github.mundanej.mjo.naming;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for local Naming Service failures. */
public final class NamingDiagnosticCodes {

  /** A supplied name or stringified-name value is malformed. */
  public static final DiagnosticCode INVALID_NAME = new DiagnosticCode("NAM-0001");

  /** A requested binding or initial naming context entry was not found. */
  public static final DiagnosticCode NOT_FOUND = new DiagnosticCode("NAM-0002");

  /** A binding already exists for the supplied name. */
  public static final DiagnosticCode ALREADY_BOUND = new DiagnosticCode("NAM-0003");

  /** A name component that must identify a context identifies an object instead. */
  public static final DiagnosticCode NOT_CONTEXT = new DiagnosticCode("NAM-0004");

  /** A context cannot be destroyed because it still contains bindings. */
  public static final DiagnosticCode NOT_EMPTY = new DiagnosticCode("NAM-0005");

  /** The requested operation targeted a destroyed naming context. */
  public static final DiagnosticCode DESTROYED = new DiagnosticCode("NAM-0006");

  /** The requested operation targeted a closed binding iterator. */
  public static final DiagnosticCode ITERATOR_CLOSED = new DiagnosticCode("NAM-0007");

  /** The requested naming URL location is outside the supported local slice. */
  public static final DiagnosticCode UNSUPPORTED_LOCATION = new DiagnosticCode("NAM-0008");

  private NamingDiagnosticCodes() {}
}
