package org.omg.CORBA.portable;

/** Dynamic skeleton response factory compatibility surface. */
public interface ResponseHandler {

  /** Creates a normal reply stream. */
  OutputStream createReply();

  /** Creates an exception reply stream. */
  OutputStream createExceptionReply();
}
