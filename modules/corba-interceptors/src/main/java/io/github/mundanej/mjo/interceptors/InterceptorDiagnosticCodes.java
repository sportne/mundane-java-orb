package io.github.mundanej.mjo.interceptors;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for local Portable Interceptor failures. */
public final class InterceptorDiagnosticCodes {

  /** An interceptor name was duplicated in one registry. */
  public static final DiagnosticCode DUPLICATE_INTERCEPTOR = new DiagnosticCode("PI-0001");

  /** A service context was duplicated without replacement. */
  public static final DiagnosticCode DUPLICATE_SERVICE_CONTEXT = new DiagnosticCode("PI-0002");

  /** An interceptor callback failed while processing a request. */
  public static final DiagnosticCode CALLBACK_FAILURE = new DiagnosticCode("PI-0003");

  private InterceptorDiagnosticCodes() {}
}
