package io.github.mundanej.mjo.repositoryid;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parsed and normalized components for an RMI-format repository ID.
 *
 * @param javaBinaryName Java binary class or interface name
 * @param hash 16-hex-digit repository hash, normalized uppercase
 * @param serialVersionUid optional 16-hex-digit serialVersionUID, normalized uppercase
 */
public record RmiRepositoryIdComponents(
    String javaBinaryName, String hash, Optional<String> serialVersionUid) {

  private static final int MAX_BINARY_NAME_LENGTH = 1_024;
  private static final Pattern JAVA_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
  private static final Pattern HEX_64 = Pattern.compile("[0-9A-Fa-f]{16}");
  private static final Set<String> JAVA_KEYWORDS =
      Set.of(
          "abstract",
          "assert",
          "boolean",
          "break",
          "byte",
          "case",
          "catch",
          "char",
          "class",
          "const",
          "continue",
          "default",
          "do",
          "double",
          "else",
          "enum",
          "extends",
          "final",
          "finally",
          "float",
          "for",
          "goto",
          "if",
          "implements",
          "import",
          "instanceof",
          "int",
          "interface",
          "long",
          "native",
          "new",
          "package",
          "private",
          "protected",
          "public",
          "return",
          "short",
          "static",
          "strictfp",
          "super",
          "switch",
          "synchronized",
          "this",
          "throw",
          "throws",
          "transient",
          "try",
          "void",
          "volatile",
          "while",
          "_");

  /** Creates validated normalized RMI repository ID components. */
  public RmiRepositoryIdComponents {
    javaBinaryName = requireJavaBinaryName(javaBinaryName);
    hash = requireHex64(hash, "hash");
    serialVersionUid =
        Objects.requireNonNull(serialVersionUid, "serialVersionUid")
            .map(value -> requireHex64(value, "serialVersionUid"));
  }

  /** Creates validated normalized RMI repository ID components. */
  public static RmiRepositoryIdComponents create(
      String javaBinaryName, String hash, Optional<String> serialVersionUid) {
    return new RmiRepositoryIdComponents(javaBinaryName, hash, serialVersionUid);
  }

  /** Returns the repository ID body after the {@code RMI:} format prefix. */
  public String body() {
    return serialVersionUid
        .map(uid -> javaBinaryName + ":" + hash + ":" + uid)
        .orElse(javaBinaryName + ":" + hash);
  }

  private static String requireJavaBinaryName(String value) {
    Objects.requireNonNull(value, "javaBinaryName");
    if (value.isBlank()) {
      throw new RepositoryIdException("RMI Java binary name must not be blank");
    }
    if (value.length() > MAX_BINARY_NAME_LENGTH) {
      throw new RepositoryIdException("RMI Java binary name is too long");
    }
    if (value.indexOf(':') >= 0) {
      throw new RepositoryIdException("RMI Java binary name must not contain ':'");
    }
    String[] segments = value.split("\\.", -1);
    for (String segment : segments) {
      if (!JAVA_IDENTIFIER.matcher(segment).matches() || JAVA_KEYWORDS.contains(segment)) {
        throw new RepositoryIdException("Invalid RMI Java binary name: " + value);
      }
    }
    return value;
  }

  private static String requireHex64(String value, String name) {
    Objects.requireNonNull(value, name);
    if (!HEX_64.matcher(value).matches()) {
      throw new RepositoryIdException(name + " must be exactly 16 hexadecimal digits");
    }
    return value.toUpperCase(Locale.ROOT);
  }
}
