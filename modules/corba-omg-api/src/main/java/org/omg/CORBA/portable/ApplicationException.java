package org.omg.CORBA.portable;

/** User exception reply received by a generated stub. */
@SuppressWarnings("serial")
public final class ApplicationException extends java.lang.Exception {

  private static final long serialVersionUID = 1L;

  private final String id;
  private final InputStream inputStream;

  /** Creates an application exception. */
  public ApplicationException(String id, InputStream inputStream) {
    super(id);
    this.id = java.util.Objects.requireNonNull(id, "id");
    this.inputStream = java.util.Objects.requireNonNull(inputStream, "inputStream");
  }

  /** Returns the repository ID. */
  public String getId() {
    return id;
  }

  /** Returns the reply input stream. */
  public InputStream getInputStream() {
    return inputStream;
  }
}
