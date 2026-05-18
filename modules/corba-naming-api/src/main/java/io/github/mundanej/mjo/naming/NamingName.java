package io.github.mundanej.mjo.naming;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Non-empty local CosNaming name.
 *
 * @param components components in traversal order
 */
public record NamingName(List<NameComponent> components) {

  /** Creates a validated immutable naming name. */
  public NamingName {
    components = List.copyOf(Objects.requireNonNull(components, "components"));
    if (components.isEmpty()) {
      throw new NamingException(NamingDiagnosticCodes.INVALID_NAME, "name must not be empty");
    }
  }

  /** Creates a name from components. */
  public static NamingName of(NameComponent first, NameComponent... rest) {
    Objects.requireNonNull(rest, "rest");
    List<NameComponent> components = new ArrayList<>(1 + rest.length);
    components.add(Objects.requireNonNull(first, "first"));
    for (NameComponent component : rest) {
      components.add(Objects.requireNonNull(component, "component"));
    }
    return new NamingName(components);
  }

  /** Parses a local stringified name using slash, dot, and backslash escaping. */
  public static NamingName parse(String value) {
    Objects.requireNonNull(value, "value");
    if (value.isEmpty()) {
      throw new NamingException(NamingDiagnosticCodes.INVALID_NAME, "name must not be empty");
    }
    List<NameComponent> components = new ArrayList<>();
    List<String> rawComponents = split(value, '/');
    for (String rawComponent : rawComponents) {
      components.add(parseComponent(rawComponent));
    }
    return new NamingName(components);
  }

  /** Returns the parent name, or empty when this name has one component. */
  public List<NameComponent> parentComponents() {
    if (components.size() == 1) {
      return List.of();
    }
    return components.subList(0, components.size() - 1);
  }

  /** Returns the final component. */
  public NameComponent leaf() {
    return components.get(components.size() - 1);
  }

  /** Returns the local stringified-name form. */
  public String stringified() {
    return components.stream()
        .map(NamingName::formatComponent)
        .reduce((a, b) -> a + "/" + b)
        .orElseThrow();
  }

  @Override
  public String toString() {
    return stringified();
  }

  private static NameComponent parseComponent(String rawComponent) {
    if (rawComponent.isEmpty()) {
      throw new NamingException(
          NamingDiagnosticCodes.INVALID_NAME, "name component must not be empty");
    }
    List<String> parts = split(rawComponent, '.');
    if (parts.size() > 2) {
      throw new NamingException(
          NamingDiagnosticCodes.INVALID_NAME, "name component has more than one unescaped dot");
    }
    String id = unescape(parts.get(0));
    String kind = parts.size() == 1 ? "" : unescape(parts.get(1));
    return new NameComponent(id, kind);
  }

  private static List<String> split(String value, char delimiter) {
    List<String> parts = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean escaping = false;
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if (escaping) {
        if (character != '/' && character != '.' && character != '\\') {
          throw new NamingException(
              NamingDiagnosticCodes.INVALID_NAME, "invalid escape sequence: \\" + character);
        }
        current.append('\\').append(character);
        escaping = false;
      } else if (character == '\\') {
        escaping = true;
      } else if (character == delimiter) {
        parts.add(current.toString());
        current.setLength(0);
      } else {
        current.append(character);
      }
    }
    if (escaping) {
      throw new NamingException(NamingDiagnosticCodes.INVALID_NAME, "incomplete escape sequence");
    }
    parts.add(current.toString());
    return parts;
  }

  private static String unescape(String value) {
    StringBuilder result = new StringBuilder(value.length());
    boolean escaping = false;
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if (escaping) {
        result.append(character);
        escaping = false;
      } else if (character == '\\') {
        escaping = true;
      } else {
        result.append(character);
      }
    }
    return result.toString();
  }

  private static String formatComponent(NameComponent component) {
    String id = escape(component.id());
    String kind = escape(component.kind());
    return component.kind().isEmpty() && !component.id().isEmpty() ? id : id + "." + kind;
  }

  private static String escape(String value) {
    StringBuilder result = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if (character == '/' || character == '.' || character == '\\') {
        result.append('\\');
      }
      result.append(character);
    }
    return result.toString();
  }
}
