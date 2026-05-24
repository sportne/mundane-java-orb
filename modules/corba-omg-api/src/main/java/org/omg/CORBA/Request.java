package org.omg.CORBA;

/** Dynamic invocation request compatibility surface. */
public abstract class Request {

  /** Returns the target object. */
  public abstract Object target();

  /** Returns the operation name. */
  public abstract String operation();

  /** Returns the argument list. */
  public abstract NVList arguments();

  /** Returns the result holder. */
  public abstract NamedValue result();

  /** Invokes the request. */
  public abstract void invoke();

  /** Sends the request without waiting for a response. */
  public abstract void send_oneway();

  /** Sends the request and returns immediately. */
  public abstract void send_deferred();

  /** Polls for a deferred response. */
  public abstract boolean poll_response();

  /** Retrieves a deferred response. */
  public abstract void get_response();
}
