package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;

/** Explicit payload contract for generated RMI-IIOP user exceptions. */
public interface RmiIiopUserExceptionPayload {

  /** Returns user-exception field values in declared IDL order. */
  List<RmiCdrValue> rmiIiopFields();
}
