package io.github.mundanej.mjo.repositoryid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Immutable parsed CORBA RepositoryId value. */
public final class RepositoryId {

  private static final Pattern IDL_PATH_CHARACTER = Pattern.compile("[A-Za-z0-9_.-]+");
  private static final Pattern DCE_BODY =
      Pattern.compile(
          "[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}:[0-9]+");

  private final RepositoryIdFormat format;
  private final String formatName;
  private final String body;
  private final Optional<RepositoryIdVersion> version;
  private final Optional<RmiRepositoryIdComponents> rmiComponents;
  private final String value;

  private RepositoryId(
      RepositoryIdFormat format,
      String formatName,
      String body,
      Optional<RepositoryIdVersion> version) {
    this(format, formatName, body, version, Optional.empty());
  }

  private RepositoryId(
      RepositoryIdFormat format,
      String formatName,
      String body,
      Optional<RepositoryIdVersion> version,
      Optional<RmiRepositoryIdComponents> rmiComponents) {
    this.format = Objects.requireNonNull(format, "format");
    this.formatName = requireFormatName(formatName);
    this.body = Objects.requireNonNull(body, "body");
    this.version = Objects.requireNonNull(version, "version");
    this.rmiComponents = Objects.requireNonNull(rmiComponents, "rmiComponents");
    if (format != RepositoryIdFormat.RMI && rmiComponents.isPresent()) {
      throw new RepositoryIdException("RMI components are only valid for RMI repository IDs");
    }
    this.value = this.formatName + ":" + this.body;
  }

  /** Parses and normalizes a repository ID string. */
  public static RepositoryId parse(String value) {
    Objects.requireNonNull(value, "value");
    int separator = value.indexOf(':');
    if (separator <= 0) {
      throw new RepositoryIdException("repository ID must have format:string form: " + value);
    }

    String formatName = requireFormatName(value.substring(0, separator));
    String body = value.substring(separator + 1);
    RepositoryIdFormat format = RepositoryIdFormat.fromFormatName(formatName);
    return switch (format) {
      case IDL -> parseIdl(formatName, body);
      case RMI -> parseRmi(formatName, body);
      case DCE -> parseDce(formatName, body);
      case LOCAL -> new RepositoryId(format, formatName, body, Optional.empty());
      case UNKNOWN -> new RepositoryId(format, formatName, body, Optional.empty());
    };
  }

  /** Creates an IDL-format repository ID from an already assembled path and version. */
  public static RepositoryId idl(String path, RepositoryIdVersion version) {
    return createIdl(path, Objects.requireNonNull(version, "version"));
  }

  /** Creates an IDL-format repository ID from scoped-name segments and a version. */
  public static RepositoryId idl(List<String> scopedName, RepositoryIdVersion version) {
    return idl(Optional.empty(), scopedName, version);
  }

  /**
   * Creates an IDL-format repository ID from an optional prefix, scoped-name segments, and version.
   */
  public static RepositoryId idl(
      Optional<String> prefix, List<String> scopedName, RepositoryIdVersion version) {
    Objects.requireNonNull(prefix, "prefix");
    Objects.requireNonNull(scopedName, "scopedName");
    Objects.requireNonNull(version, "version");
    if (scopedName.isEmpty()) {
      throw new RepositoryIdException("scoped name must contain at least one segment");
    }

    List<String> pathSegments = new ArrayList<>();
    prefix.ifPresent(pathSegments::add);
    pathSegments.addAll(scopedName);
    return createIdl(String.join("/", pathSegments), version);
  }

  /** Creates an RMI-format repository ID from explicit Java binary name and hash inputs. */
  public static RepositoryId rmi(String javaBinaryName, String hash) {
    return rmi(javaBinaryName, hash, Optional.empty());
  }

  /** Creates an RMI-format repository ID with an explicit serialVersionUID component. */
  public static RepositoryId rmi(String javaBinaryName, String hash, String serialVersionUid) {
    return rmi(javaBinaryName, hash, Optional.of(serialVersionUid));
  }

  /** Creates an RMI-format repository ID with optional explicit serialVersionUID input. */
  public static RepositoryId rmi(
      String javaBinaryName, String hash, Optional<String> serialVersionUid) {
    RmiRepositoryIdComponents components =
        RmiRepositoryIdComponents.create(javaBinaryName, hash, serialVersionUid);
    return new RepositoryId(
        RepositoryIdFormat.RMI,
        "RMI",
        components.body(),
        Optional.empty(),
        Optional.of(components));
  }

  /** Creates a syntactically valid generic repository ID. */
  public static RepositoryId generic(String formatName, String body) {
    return parse(requireFormatName(formatName) + ":" + Objects.requireNonNull(body, "body"));
  }

  /** Returns the recognized repository ID format, or {@link RepositoryIdFormat#UNKNOWN}. */
  public RepositoryIdFormat format() {
    return format;
  }

  /** Returns the exact repository ID format name. */
  public String formatName() {
    return formatName;
  }

  /** Returns the repository ID body after the first colon. */
  public String body() {
    return body;
  }

  /** Returns the IDL version when this is an IDL-format repository ID. */
  public Optional<RepositoryIdVersion> version() {
    return version;
  }

  /** Returns parsed RMI components when this is an RMI-format repository ID. */
  public Optional<RmiRepositoryIdComponents> rmiComponents() {
    return rmiComponents;
  }

  /** Returns the deterministic repository ID string. */
  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof RepositoryId repositoryId && value.equals(repositoryId.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  private static RepositoryId parseIdl(String formatName, String body) {
    int versionSeparator = body.lastIndexOf(':');
    if (versionSeparator <= 0 || versionSeparator == body.length() - 1) {
      throw new RepositoryIdException("IDL repository ID must be IDL:path:major.minor");
    }
    String path = body.substring(0, versionSeparator);
    RepositoryIdVersion version = RepositoryIdVersion.parse(body.substring(versionSeparator + 1));
    return createIdl(path, version, formatName);
  }

  private static RepositoryId parseRmi(String formatName, String body) {
    String[] components = body.split(":", -1);
    if (components.length != 2 && components.length != 3) {
      throw new RepositoryIdException("RMI repository ID must have class, hash, and optional UID");
    }
    RmiRepositoryIdComponents rmiComponents =
        RmiRepositoryIdComponents.create(
            components[0],
            components[1],
            components.length == 3 ? Optional.of(components[2]) : Optional.empty());
    return new RepositoryId(
        RepositoryIdFormat.RMI,
        formatName,
        rmiComponents.body(),
        Optional.empty(),
        Optional.of(rmiComponents));
  }

  private static RepositoryId parseDce(String formatName, String body) {
    if (!DCE_BODY.matcher(body).matches()) {
      throw new RepositoryIdException("DCE repository ID must be DCE:uuid:minor-version");
    }
    return new RepositoryId(RepositoryIdFormat.DCE, formatName, body, Optional.empty());
  }

  private static RepositoryId createIdl(String path, RepositoryIdVersion version) {
    return createIdl(path, version, "IDL");
  }

  private static RepositoryId createIdl(
      String path, RepositoryIdVersion version, String formatName) {
    validateIdlPath(path);
    String normalizedBody = path + ":" + version;
    return new RepositoryId(
        RepositoryIdFormat.IDL, formatName, normalizedBody, Optional.of(version));
  }

  private static void validateIdlPath(String path) {
    Objects.requireNonNull(path, "path");
    if (path.isBlank()) {
      throw new RepositoryIdException("IDL repository ID path must not be blank");
    }
    if (path.endsWith("/")) {
      throw new RepositoryIdException("IDL repository ID path must not have a trailing slash");
    }
    char first = path.charAt(0);
    if (first == '_' || first == '-' || first == '.') {
      throw new RepositoryIdException("IDL repository ID path must not start with _, -, or .");
    }
    String[] segments = path.split("/", -1);
    for (String segment : segments) {
      if (segment.isEmpty()) {
        throw new RepositoryIdException("IDL repository ID path segments must not be empty");
      }
      if (!IDL_PATH_CHARACTER.matcher(segment).matches()) {
        throw new RepositoryIdException(
            "IDL repository ID path segment has invalid characters: " + segment);
      }
    }
  }

  private static String requireFormatName(String value) {
    Objects.requireNonNull(value, "formatName");
    if (value.isBlank()) {
      throw new RepositoryIdException("repository ID format must not be blank");
    }
    if (value.indexOf(':') >= 0) {
      throw new RepositoryIdException("repository ID format must not contain ':'");
    }
    return value;
  }
}
