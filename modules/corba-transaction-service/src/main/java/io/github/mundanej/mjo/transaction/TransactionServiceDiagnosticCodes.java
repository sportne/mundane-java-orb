package io.github.mundanej.mjo.transaction;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for supported Transaction Service failures. */
public final class TransactionServiceDiagnosticCodes {

  /** A configured Transaction Service limit was outside the supported range. */
  public static final DiagnosticCode INVALID_LIMIT = new DiagnosticCode("TXN-0001");

  /** A transaction or resource identifier was blank, missing, or oversized. */
  public static final DiagnosticCode MALFORMED_IDENTIFIER = new DiagnosticCode("TXN-0002");

  /** A transaction creation used an ID that already exists. */
  public static final DiagnosticCode TRANSACTION_ALREADY_EXISTS = new DiagnosticCode("TXN-0003");

  /** A transaction lookup or mutation referenced an unknown transaction. */
  public static final DiagnosticCode TRANSACTION_NOT_FOUND = new DiagnosticCode("TXN-0004");

  /** The configured transaction count has been reached. */
  public static final DiagnosticCode TRANSACTION_LIMIT_EXCEEDED = new DiagnosticCode("TXN-0005");

  /** A resource enlistment used an ID that is already enlisted. */
  public static final DiagnosticCode RESOURCE_ALREADY_ENLISTED = new DiagnosticCode("TXN-0006");

  /** A resource lookup or delist operation referenced an unknown resource. */
  public static final DiagnosticCode RESOURCE_NOT_FOUND = new DiagnosticCode("TXN-0007");

  /** The configured resource count for one transaction has been reached. */
  public static final DiagnosticCode RESOURCE_LIMIT_EXCEEDED = new DiagnosticCode("TXN-0008");

  /** A transaction handle no longer matches the current local transaction. */
  public static final DiagnosticCode STALE_TRANSACTION = new DiagnosticCode("TXN-0009");

  /** A resource handle no longer matches the current local enlistment. */
  public static final DiagnosticCode STALE_RESOURCE = new DiagnosticCode("TXN-0010");

  /** A requested transaction timeout was outside the supported policy bounds. */
  public static final DiagnosticCode INVALID_TIMEOUT = new DiagnosticCode("TXN-0011");

  /** A local transaction has reached its configured deadline. */
  public static final DiagnosticCode TRANSACTION_EXPIRED = new DiagnosticCode("TXN-0012");

  private TransactionServiceDiagnosticCodes() {}
}
