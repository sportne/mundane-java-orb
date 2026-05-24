package org.omg.CORBA.portable;

/** Dynamic skeleton invocation handler compatibility surface. */
public interface InvokeHandler {

  /** Invokes a named operation. */
  OutputStream _invoke(String operation, InputStream input, ResponseHandler handler);
}
