package org.omg.CORBA;

/** Dynamic skeleton server request compatibility surface. */
public abstract class ServerRequest {

  /** Returns the operation name. */
  public abstract String operation();

  /** Provides expected arguments. */
  public abstract void arguments(NVList arguments);

  /** Sets the result value. */
  public abstract void set_result(Any value);

  /** Sets the exception value. */
  public abstract void set_exception(Any value);
}
