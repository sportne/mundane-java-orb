package org.omg.CORBA;

/** Completion status carried by CORBA system exceptions. */
public enum CompletionStatus {
  /** The operation completed before the exception was raised. */
  COMPLETED_YES,

  /** The operation did not complete before the exception was raised. */
  COMPLETED_NO,

  /** Completion state is unknown. */
  COMPLETED_MAYBE
}
