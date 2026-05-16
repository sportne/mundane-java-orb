package io.github.mundanej.mjo.repositoryid;

import java.util.Objects;

/**
 * Nonnegative major/minor version for IDL-format repository IDs.
 *
 * @param major major version number
 * @param minor minor version number
 */
public record RepositoryIdVersion(long major, long minor) {

  /** Creates a validated version. */
  public RepositoryIdVersion {
    if (major < 0) {
      throw new RepositoryIdException("major version must be nonnegative");
    }
    if (minor < 0) {
      throw new RepositoryIdException("minor version must be nonnegative");
    }
  }

  /** Parses a decimal {@code major.minor} version string. */
  public static RepositoryIdVersion parse(String value) {
    Objects.requireNonNull(value, "value");
    int separator = value.indexOf('.');
    if (separator <= 0
        || separator == value.length() - 1
        || value.indexOf('.', separator + 1) >= 0) {
      throw new RepositoryIdException("repository ID version must be major.minor: " + value);
    }
    return new RepositoryIdVersion(
        parseNonnegativeLong(value.substring(0, separator), "major"),
        parseNonnegativeLong(value.substring(separator + 1), "minor"));
  }

  @Override
  public String toString() {
    return major + "." + minor;
  }

  private static long parseNonnegativeLong(String value, String componentName) {
    if (!value.chars().allMatch(Character::isDigit)) {
      throw new RepositoryIdException(componentName + " version must be decimal digits: " + value);
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      throw new RepositoryIdException(componentName + " version is too large: " + value);
    }
  }
}
