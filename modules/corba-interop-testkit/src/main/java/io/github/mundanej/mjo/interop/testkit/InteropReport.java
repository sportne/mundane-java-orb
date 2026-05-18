package io.github.mundanej.mjo.interop.testkit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable clean-room interop report with deterministic JSON encoding. */
public record InteropReport(
    String peer,
    String peerVersion,
    String scenario,
    String idl,
    String clientRuntime,
    String serverRuntime,
    InteropRole role,
    String image,
    String command,
    InteropReportStatus status,
    InteropFailureClassification classification,
    int exitCode,
    String stdoutPath,
    String stderrPath,
    String reportPath,
    String startedAt,
    String endedAt,
    String notes) {
  public InteropReport {
    requireNotBlank(peer, "peer");
    requireNotBlank(peerVersion, "peerVersion");
    requireNotBlank(scenario, "scenario");
    requireNotBlank(idl, "idl");
    requireNotBlank(clientRuntime, "clientRuntime");
    requireNotBlank(serverRuntime, "serverRuntime");
    Objects.requireNonNull(role, "role");
    requireNotBlank(image, "image");
    requireNotBlank(command, "command");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(classification, "classification");
    requireNotBlank(stdoutPath, "stdoutPath");
    requireNotBlank(stderrPath, "stderrPath");
    requireNotBlank(reportPath, "reportPath");
    requireNotBlank(startedAt, "startedAt");
    requireNotBlank(endedAt, "endedAt");
    notes = notes == null ? "" : notes;
  }

  public String toJson() {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("peer", peer);
    fields.put("peerVersion", peerVersion);
    fields.put("scenario", scenario);
    fields.put("idl", idl);
    fields.put("clientRuntime", clientRuntime);
    fields.put("serverRuntime", serverRuntime);
    fields.put("role", role.wireName());
    fields.put("image", image);
    fields.put("command", command);
    fields.put("status", status.wireName());
    fields.put("classification", classification.wireName());
    fields.put("exitCode", Integer.toString(exitCode));
    fields.put("stdoutPath", stdoutPath);
    fields.put("stderrPath", stderrPath);
    fields.put("reportPath", reportPath);
    fields.put("startedAt", startedAt);
    fields.put("endedAt", endedAt);
    fields.put("notes", notes);
    StringBuilder builder = new StringBuilder("{\n");
    int index = 0;
    for (Map.Entry<String, String> entry : fields.entrySet()) {
      builder.append("  \"").append(entry.getKey()).append("\": ");
      if ("exitCode".equals(entry.getKey())) {
        builder.append(entry.getValue());
      } else {
        builder.append('"').append(escape(entry.getValue())).append('"');
      }
      if (++index < fields.size()) {
        builder.append(',');
      }
      builder.append('\n');
    }
    return builder.append("}\n").toString();
  }

  public static InteropReport fromJson(String json) {
    Map<String, String> fields = parseFlatJson(json);
    return new InteropReport(
        required(fields, "peer"),
        required(fields, "peerVersion"),
        required(fields, "scenario"),
        required(fields, "idl"),
        required(fields, "clientRuntime"),
        required(fields, "serverRuntime"),
        InteropRole.fromWireName(required(fields, "role")),
        required(fields, "image"),
        required(fields, "command"),
        InteropReportStatus.fromWireName(required(fields, "status")),
        InteropFailureClassification.fromWireName(required(fields, "classification")),
        Integer.parseInt(required(fields, "exitCode")),
        required(fields, "stdoutPath"),
        required(fields, "stderrPath"),
        required(fields, "reportPath"),
        required(fields, "startedAt"),
        required(fields, "endedAt"),
        fields.getOrDefault("notes", ""));
  }

  private static Map<String, String> parseFlatJson(String json) {
    if (json == null || json.isBlank()) {
      throw new IllegalArgumentException("json must not be blank");
    }
    Map<String, String> fields = new LinkedHashMap<>();
    String content = json.strip();
    if (!content.startsWith("{") || !content.endsWith("}")) {
      throw new IllegalArgumentException("json object expected");
    }
    int cursor = 1;
    while (cursor < content.length() - 1) {
      cursor = skipWhitespaceAndComma(content, cursor);
      if (cursor >= content.length() - 1) {
        break;
      }
      ParsedString key = parseJsonString(content, cursor);
      cursor = skipWhitespace(content, key.nextIndex());
      if (cursor >= content.length() || content.charAt(cursor) != ':') {
        throw new IllegalArgumentException("expected ':' after json key");
      }
      cursor = skipWhitespace(content, cursor + 1);
      String value;
      if (content.charAt(cursor) == '"') {
        ParsedString parsedValue = parseJsonString(content, cursor);
        value = parsedValue.value();
        cursor = parsedValue.nextIndex();
      } else {
        int valueStart = cursor;
        while (cursor < content.length() && ",}\n\r\t ".indexOf(content.charAt(cursor)) < 0) {
          cursor++;
        }
        value = content.substring(valueStart, cursor);
      }
      fields.put(key.value(), value);
    }
    return fields;
  }

  private static ParsedString parseJsonString(String text, int start) {
    if (text.charAt(start) != '"') {
      throw new IllegalArgumentException("expected json string");
    }
    StringBuilder builder = new StringBuilder();
    int cursor = start + 1;
    while (cursor < text.length()) {
      char item = text.charAt(cursor++);
      if (item == '"') {
        return new ParsedString(builder.toString(), cursor);
      }
      if (item == '\\') {
        if (cursor >= text.length()) {
          throw new IllegalArgumentException("unterminated json escape");
        }
        char escaped = text.charAt(cursor++);
        builder.append(
            switch (escaped) {
              case '"', '\\', '/' -> escaped;
              case 'n' -> '\n';
              case 'r' -> '\r';
              case 't' -> '\t';
              default -> throw new IllegalArgumentException("unsupported json escape: " + escaped);
            });
      } else {
        builder.append(item);
      }
    }
    throw new IllegalArgumentException("unterminated json string");
  }

  private static int skipWhitespaceAndComma(String text, int cursor) {
    while (cursor < text.length()
        && (Character.isWhitespace(text.charAt(cursor)) || text.charAt(cursor) == ',')) {
      cursor++;
    }
    return cursor;
  }

  private static int skipWhitespace(String text, int cursor) {
    while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
      cursor++;
    }
    return cursor;
  }

  private static String required(Map<String, String> fields, String fieldName) {
    String value = fields.get(fieldName);
    if (value == null) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value;
  }

  private static String escape(String value) {
    StringBuilder builder = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char item = value.charAt(index);
      switch (item) {
        case '"' -> builder.append("\\\"");
        case '\\' -> builder.append("\\\\");
        case '\n' -> builder.append("\\n");
        case '\r' -> builder.append("\\r");
        case '\t' -> builder.append("\\t");
        default -> builder.append(item);
      }
    }
    return builder.toString();
  }

  private static void requireNotBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }

  private record ParsedString(String value, int nextIndex) {}
}
