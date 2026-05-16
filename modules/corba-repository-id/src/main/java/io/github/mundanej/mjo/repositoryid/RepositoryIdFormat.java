package io.github.mundanej.mjo.repositoryid;

/** Known CORBA RepositoryId format names. */
public enum RepositoryIdFormat {
  /** IDL scoped-name format from CORBA Interface Repository 14.7.1. */
  IDL,
  /** Java RMI hashed format from CORBA Interface Repository 14.7.2. */
  RMI,
  /** DCE UUID format from CORBA Interface Repository 14.7.3. */
  DCE,
  /** Local repository-private format from CORBA Interface Repository 14.7.4. */
  LOCAL,
  /** A syntactically valid format name not recognized by this foundation slice. */
  UNKNOWN;

  static RepositoryIdFormat fromFormatName(String formatName) {
    return switch (formatName) {
      case "IDL" -> IDL;
      case "RMI" -> RMI;
      case "DCE" -> DCE;
      case "LOCAL" -> LOCAL;
      default -> UNKNOWN;
    };
  }
}
